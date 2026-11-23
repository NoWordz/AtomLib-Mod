package com.atom.lib.controls;

import com.atom.lib.geometry.*;
import com.atom.lib.input.*;
import com.atom.lib.layout.NodeId;
import com.atom.lib.design.atomchat.AtomChatControlStyle;
import io.github.humbleui.skija.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/** Compiled against a released library, then run unchanged against both releases. */
public final class CollectionCompatibilityProbe {
    public static void main(String[] args) throws Exception {
        var output = Path.of(args[0]);
        Files.createDirectories(output);
        int samples = 0;
        for (boolean alternate : List.of(false, true)) {
            for (float density : new float[]{1, 1.5f, 2}) {
                for (String mode : List.of("grid", "grid-hover", "table", "table-sorted", "table-scroll", "no-context")) {
                    render(output, alternate, density, mode);
                    samples++;
                }
            }
        }
        System.out.println("PASS released binary collection scenes=" + samples);
    }

    private static void render(Path output, boolean alternate, float density, String mode) throws Exception {
        var values = new ArrayList<Integer>();
        for (int i = 0; i < 30; i++) values.add(i);
        var data = new VirtualData<Integer,Integer>() {
            public int count() { return values.size(); }
            public Integer itemAt(int index) { return values.get(index); }
            public Integer key(Integer value) { return value; }
        };
        boolean table = mode.startsWith("table");
        try (var fixture = new ControlFixture();
             var collection = table
                     ? VirtualCollection.table(new NodeId("compat"), data, 40, List.of(new TableColumn("name", "名称 Name", 160, true), new TableColumn("action", "Action", 160, false)))
                     : VirtualCollection.grid(new NodeId("compat"), data, 100, 60, 10);
             var surface = Surface.makeRasterN32Premul((int)(320*density), (int)(220*density))) {
            if (alternate) fixture.skin.updateStyle(new AtomChatControlStyle(30, false, 0xffa78bfa, 0xffb3b8cd, .7f, 0xff2b2e3d, 0xff232633, 0xfff5f6fa));
            var viewport = new Viewport(320, 220, density);
            var bounds = new Rect(0, 0, 320, 220);
            var frame = collection.layout(viewport, bounds);
            var canvas = surface.getCanvas();
            canvas.scale(density, density);
            VirtualCollection.ItemPainter<Integer,Integer> painter = (c, cell, rect) -> {
                try (var paint = new Paint().setColor(0xff7b8a99)) {
                    c.drawRect(io.github.humbleui.types.Rect.makeXYWH(rect.x()+8, rect.y()+10, 9+cell.key()%5, 12), paint);
                }
            };
            if (mode.equals("no-context")) collection.paint(canvas, frame, painter);
            else collection.paint(canvas, frame, fixture.context, painter);
            if (mode.equals("grid-hover")) collection.hover(20, 30);
            if (mode.equals("table-sorted")) {
                collection.pointer(new PointerEvent(PointerEvent.Type.PRESS, 10, 10, 0));
                collection.pointer(new PointerEvent(PointerEvent.Type.RELEASE, 10, 10, 0));
            }
            if (mode.equals("table-scroll")) collection.scrollBy(120);
            canvas.clear(0xff192029);
            frame = collection.layout(viewport, bounds);
            if (mode.equals("no-context")) collection.paint(canvas, frame, painter);
            else collection.paint(canvas, frame, fixture.context, painter);
            var name = (alternate ? "alternate" : "default") + "-" + density + "-" + mode;
            try (var image = surface.makeImageSnapshot(); var bitmap = Bitmap.makeFromImage(image);
                 var pixels = new DataOutputStream(Files.newOutputStream(output.resolve(name + ".argb")));
                 var png = image.encodeToData()) {
                for (int y = 0; y < bitmap.getHeight(); y++) for (int x = 0; x < bitmap.getWidth(); x++) pixels.writeInt(bitmap.getColor(x, y));
                Files.write(output.resolve(name + ".png"), png.getBytes());
            }
            if (frame.cells().isEmpty() || collection.hit(20, table ? 60 : 30) == null) throw new AssertionError("Empty or unconnected collection scene");
        }
    }
}
