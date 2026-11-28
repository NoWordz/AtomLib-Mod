// SPDX-License-Identifier: Apache-2.0
package com.atom.example.a;

import com.atom.lib.AtomLibRuntime;
import com.atom.lib.font.FontRegistry;
import com.atom.lib.geometry.Rect;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;
import io.github.humbleui.skija.Surface;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

/** Runs the native settings example from the published Maven coordinate of each loader. */
public final class PreviewSettingsExample {
    private static final Rect PANEL = new Rect(24, 18, 354, 246);
    private static final int WIDTH = 402, HEIGHT = 282;

    public static void main(String[] arguments) throws Exception {
        var directory = Path.of(arguments[0]);
        Files.createDirectories(directory);
        try (var runtime = AtomLibRuntime.open("preview-settings-example");
             var fonts = new FontRegistry(runtime);
             var background = Surface.makeRasterN32Premul(WIDTH, HEIGHT);
             var screen = Surface.makeRasterN32Premul(WIDTH, HEIGHT);
             var page = new PreviewSettingsPage()) {
            fonts.registerDefaults();
            try (var gradient = Shader.makeLinearGradient(0, 0, WIDTH, HEIGHT,
                    new int[]{0xff405594, 0xffc67984, 0xff354e72});
                 var paint = new Paint().setShader(gradient)) {
                background.getCanvas().drawPaint(paint);
            }
            try (var backdrop = background.makeImageSnapshot();
                 var font = fonts.openFont(FontRegistry.DEFAULT_REGULAR, 13, "Appearance 外观动效青柠")) {
                byte[] initial = draw(page, screen, backdrop, font, 0, false);
                Files.write(directory.resolve("01-lime.png"), initial);
                require(page.wheel(130, 145, 88), "Could not scroll the settings body");
                byte[] scrolled = draw(page, screen, backdrop, font, 16, false);
                require(!Arrays.equals(initial, scrolled), "Scrolling did not change the body");
                Files.write(directory.resolve("02-scrolled.png"), scrolled);
                require(page.click(176, 42), "Motion tab is missing");
                draw(page, screen, backdrop, font, 20, false);
                require(!page.click(320, 86), "An invisible arriving control took input");
                require(!page.wheel(230, 145, 40), "An invisible arriving page scrolled");
                byte[] mixed = draw(page, screen, backdrop, font, 94, false);
                Files.write(directory.resolve("03-switching.png"), mixed);
                require(page.click(68, 42), "Fixed tab cannot interrupt motion");
                byte[] interrupted = draw(page, screen, backdrop, font, 100, false);
                require(Arrays.equals(mixed, interrupted) ||
                                Arrays.equals(bodyPixels(mixed), bodyPixels(interrupted)),
                        "Interrupted body jumped before the next transition had advanced");
                draw(page, screen, backdrop, font, 350, false);
                page.wheel(130, 145, -1000);
                draw(page, screen, backdrop, font, 366, false);
                for (int i = 0; i < 7; i++) {
                    require(page.click(320, 81), "Theme choice not reachable at the scroll start");
                    draw(page, screen, backdrop, font, 382 + i * 16, false);
                }
                Files.write(directory.resolve("04-ice-glass-zh.png"), draw(page, screen, backdrop, font, 510, true));
            }
        }
        System.out.println("PASS independent Preview settings example");
    }

    private static byte[] draw(PreviewSettingsPage page, Surface screen, Image backdrop,
                               io.github.humbleui.skija.Font font, long ms, boolean chinese) {
        screen.getCanvas().drawImage(backdrop, 0, 0);
        page.paint(screen.getCanvas(), PANEL, font, backdrop, chinese, new FrameTime(ms, ms * 1_000_000, 0));
        try (var snapshot = screen.makeImageSnapshot(); var png = snapshot.encodeToData()) {
            return png.getBytes();
        }
    }

    private static int[] bodyPixels(byte[] png) throws Exception {
        var image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(png));
        // Fixed tab trim is outside the body and may update immediately.
        return image.getRGB(36, 66, 330, 185, null, 0, 330);
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
