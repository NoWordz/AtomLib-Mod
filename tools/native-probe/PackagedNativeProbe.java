// SPDX-License-Identifier: Apache-2.0
import com.atom.lib.AtomLibRuntime;
import com.atom.lib.NativePreparation;
import com.atom.lib.geometry.Rect;
import com.atom.lib.geometry.Viewport;
import com.atom.lib.layout.LayoutNode;
import com.atom.lib.layout.LayoutStyle;
import com.atom.lib.layout.LayoutTree;
import com.atom.lib.layout.Length;
import com.atom.lib.layout.NodeId;
import com.atom.lib.layout.internal.YogaLayoutEngine;
import com.atom.lib.layout.internal.YogaNatives;
import com.atom.lib.natives.ClientNatives;
import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.ImageInfo;
import io.github.humbleui.skija.Surface;

import java.nio.file.Path;
import java.util.List;

public final class PackagedNativeProbe {
    public static void main(String[] args) throws Exception {
        Path game = Path.of(args[0]);
        String platform = args[1];
        int javaVersion = Integer.parseInt(args[2]);
        String source = System.getProperty("atomlib.natives.source");
        if (Runtime.version().feature() != javaVersion) {
            throw new AssertionError("Expected Java " + javaVersion + ", got " + Runtime.version());
        }
        System.out.printf("%s %s, JVM %s, native source %s%n", System.getProperty("os.name"),
                System.getProperty("os.arch"), Runtime.version(), source);

        ClientNatives.start(game, YogaNatives::hasConfiguredNative, YogaNatives::usePreparedNative,
                YogaNatives::initializePreparedNative);
        NativePreparation.Status status = awaitPreparation();
        if (args[3].equals("cold-offline")) {
            if (!platform.equals(status.platform()) || status.state() != NativePreparation.State.FAILED
                    || !status.detail().startsWith("Native cache missing or damaged.")) {
                throw new AssertionError("Expected an offline cache miss for " + platform, status.failure());
            }
            System.out.println("PASS " + platform + " empty cache in offline mode");
            return;
        }
        if (!platform.equals(status.platform()) || status.state() != NativePreparation.State.AVAILABLE) {
            throw new AssertionError("Expected available " + platform + ", got " + status, status.failure());
        }

        try (var runtime = AtomLibRuntime.open("packaged-native-probe")) {
            probeRaster();
            probeLayout();
        }
        System.out.println("PASS " + platform + " " + source + " Skija raster and Yoga layout");
    }

    private static NativePreparation.Status awaitPreparation() throws InterruptedException {
        String previous = "";
        NativePreparation.Status status;
        while ((status = NativePreparation.status()).state() == NativePreparation.State.PREPARING) {
            if (!status.detail().equals(previous)) {
                System.out.println(status.detail());
                previous = status.detail();
            }
            Thread.sleep(40);
        }
        return status;
    }

    private static void probeRaster() {
        int color = 0xffab12cd;
        try (var surface = Surface.makeRasterN32Premul(8, 8); var pixels = new Bitmap()) {
            surface.getCanvas().clear(color);
            if (!pixels.allocPixels(ImageInfo.makeN32Premul(8, 8))
                    || !surface.readPixels(pixels, 0, 0) || pixels.getColor(3, 3) != color) {
                throw new AssertionError("Skija did not read back the rasterized color");
            }
        }
    }

    private static void probeLayout() {
        var child = new LayoutNode(new NodeId("child"),
                LayoutStyle.column().size(Length.auto(), Length.points(20)), List.of(), null, 0);
        var root = new LayoutNode(new NodeId("root"),
                LayoutStyle.column().size(Length.points(100), Length.points(60)), List.of(child), null, 0);
        try (var engine = new YogaLayoutEngine()) {
            var layout = engine.layout(new LayoutTree(root), new Viewport(200, 120, 1));
            if (!new Rect(0, 0, 100, 20).equals(layout.box(child.id()).border())) {
                throw new AssertionError("Unexpected Yoga layout: " + layout.box(child.id()).border());
            }
        }
    }
}
