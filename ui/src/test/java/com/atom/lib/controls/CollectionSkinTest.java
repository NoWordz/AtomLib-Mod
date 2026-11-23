package com.atom.lib.controls;

import com.atom.lib.geometry.*;
import com.atom.lib.layout.NodeId;
import com.atom.lib.text.TextService;
import io.github.humbleui.skija.*;
import java.lang.reflect.Proxy;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CollectionSkinTest {
    private static final Viewport VIEW = new Viewport(200, 100, 1);
    private static final Rect BOUNDS = new Rect(0, 0, 200, 100);

    private static final class Chrome implements CollectionSkin {
        int items, headers, labels, scrollbars;
        boolean fail;
        public void paintItem(Canvas canvas, Rect bounds, boolean grid, boolean hovered) {
            items++;
            fill(canvas, bounds, hovered ? 0xffffff00 : 0xffff0000);
        }
        public void paintHeader(Canvas canvas, Rect bounds) {
            if (fail) throw new IllegalStateException("custom chrome failure");
            headers++;
            fill(canvas, bounds, 0xff0000ff);
        }
        public void paintHeaderText(Canvas canvas, Rect bounds, String label, TextService text) {
            assertNotNull(text);
            assertFalse(label.isBlank());
            labels++;
        }
        public void paintScrollbar(Canvas canvas, Rect bounds, float opacity) {
            scrollbars++;
            fill(canvas, bounds, 0xff00ff00);
        }
    }

    private static void fill(Canvas canvas, Rect bounds, int color) {
        try (var paint = new Paint().setColor(color)) {
            canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(bounds.x(), bounds.y(), bounds.width(), bounds.height()), paint);
        }
    }

    private static ControlSkin skin(Chrome chrome) {
        return (ControlSkin) Proxy.newProxyInstance(ControlSkin.class.getClassLoader(), new Class<?>[]{ControlSkin.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("collectionSkin")) return chrome;
                    throw new AssertionError("Collection must use its skin interface: " + method.getName());
                });
    }

    private static VirtualData<Integer,Integer> data(boolean reversed) {
        return new VirtualData<>() {
            public int count() { return 20; }
            public Integer itemAt(int index) { return reversed ? 19 - index : index; }
            public Integer key(Integer value) { return value; }
        };
    }

    @Test void customSkinPaintsTheActualHeaderItemsAndScrollbar() {
        var chrome = new Chrome();
        try (var fixture = new ControlFixture();
             var view = VirtualCollection.table(new NodeId("custom"), data(false), 20, List.of(new TableColumn("name", "Name", 200, true)));
             var surface = Surface.makeRasterN32Premul(200, 100)) {
            var frame = view.layout(VIEW, BOUNDS);
            view.paint(surface.getCanvas(), frame, new ControlContext(fixture.text, skin(chrome)), (c, item, bounds) -> {});
            assertEquals(frame.cells().size(), chrome.items);
            assertEquals(1, chrome.headers);
            assertEquals(1, chrome.labels);
            assertEquals(1, chrome.scrollbars);
            try (var image = surface.makeImageSnapshot(); var bitmap = Bitmap.makeFromImage(image)) {
                assertEquals(0xff0000ff, bitmap.getColor(5, 5));
                assertEquals(0xffff0000, bitmap.getColor(5, 30));
                var thumb = frame.thumb();
                assertEquals(0xff00ff00, bitmap.getColor((int)(thumb.x() + thumb.width()/2), (int)(thumb.y() + thumb.height()/2)));
            }
            assertEquals(0, view.hit(5, 30).key());
        }
    }

    @Test void failedCustomChromeDoesNotPublishTheNewHitFrame() {
        var chrome = new Chrome();
        try (var fixture = new ControlFixture();
             var view = VirtualCollection.table(new NodeId("custom"), data(false), 20, List.of(new TableColumn("name", "Name", 200, true)));
             var surface = Surface.makeRasterN32Premul(200, 100)) {
            var context = new ControlContext(fixture.text, skin(chrome));
            view.paint(surface.getCanvas(), view.layout(VIEW, BOUNDS), context, (c, item, bounds) -> {});
            view.data(data(true));
            chrome.fail = true;
            assertThrows(IllegalStateException.class, () -> view.paint(surface.getCanvas(), view.layout(VIEW, BOUNDS), context, (c, item, bounds) -> {}));
            assertEquals(0, view.hit(5, 30).key());
        }
    }
}
