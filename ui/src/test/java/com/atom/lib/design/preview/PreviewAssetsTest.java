package com.atom.lib.design.preview;

import com.atom.lib.design.atomdefault.AtomDefault;
import com.atom.lib.geometry.Rect;
import io.github.humbleui.skija.Bitmap;
import io.github.humbleui.skija.Surface;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PreviewAssetsTest {
    @Test void previewCatalogIsSeparateAndKeepsAllSurfaceRoles() {
        var themes = PreviewThemes.catalog().presets();
        assertEquals(List.of("minimal", "summer", "elegant", "raven", "hub", "vivid",
                "dusk", "mint", "ice_glass", "sand_glass"), themes.stream().map(p -> p.id()).toList());
        assertEquals(12, AtomDefault.catalog().presets().size());
        assertEquals(0xfffafcf3, AtomDefault.catalog().find("summer").panelBackgroundColor());
        var lime = PreviewThemes.find("summer");
        assertEquals(0xffffffec, lime.palette().panelBackgroundColor());
        assertEquals(0xffecf4d4, lime.palette().cardColor());
        assertEquals(0xffd8e8b6, lime.capsuleBackground());
        assertEquals(0xff466c00, lime.capsuleText());
        assertEquals(0xd9c4dce7, PreviewThemes.find("ice_glass").capsuleBackground());
        assertEquals(4, themes.stream().filter(p -> p.blurEnabled() && p.panelOpacity() == .7f).count());
        for (var theme : themes.subList(0, 6)) {
            assertNotEquals(theme.panelBackgroundColor(), theme.cardColor());
            assertNotEquals(theme.panelBackgroundColor(), PreviewThemes.find(theme.id()).capsuleBackground());
            assertNotEquals(theme.cardColor(), PreviewThemes.find(theme.id()).capsuleBackground());
        }
    }

    @Test void fadeAffectsBothChannelsAndRecoversReachableExtremes() {
        Rect bounds = new Rect(2, 4, 20, 30);
        try (var fade = new PreviewScrollFade();
             var content = Surface.makeRasterN32Premul(24, 38);
             var mask = Surface.makeRasterN32Premul(24, 38)) {
            var middle = fade.window(bounds, 20, 40);
            fade.paint(content.getCanvas(), middle, c -> c.clear(0xff4279ff));
            fade.paint(mask.getCanvas(), middle, c -> c.clear(0xffffffff));
            assertEquals(0, alpha(content, 8, 3));
            assertEquals(0, alpha(content, 8, 34));
            assertTrue(alpha(content, 8, 4) < alpha(content, 8, 9));
            assertTrue(alpha(content, 8, 9) < alpha(content, 8, 18));
            assertTrue(alpha(content, 8, 28) > alpha(content, 8, 33));
            for (int y = 4; y < 34; y++) assertEquals(alpha(content, 8, y), alpha(mask, 8, y));
            assertEquals(1f, fade.window(bounds, 1, 40).top());
            content.getCanvas().clear(0);
            fade.paint(content.getCanvas(), fade.window(bounds, 0, 40), c -> c.clear(0xffffffff));
            assertEquals(255, alpha(content, 8, 4));
            content.getCanvas().clear(0);
            fade.paint(content.getCanvas(), fade.window(bounds, 40, 40), c -> c.clear(0xffffffff));
            assertEquals(255, alpha(content, 8, 33));
            var shortArea = fade.window(new Rect(0, 0, 8, 6), 10, 20);
            assertEquals(3f, shortArea.top());
            assertEquals(3f, shortArea.bottom());
        }
    }

    @Test void fadeRestoresCanvasAfterAHostDrawFails() {
        try (var fade = new PreviewScrollFade(); var surface = Surface.makeRasterN32Premul(28, 28)) {
            var canvas = surface.getCanvas();
            int before = canvas.getSaveCount();
            var window = fade.window(new Rect(0, 1, 24, 24), 12, 24);
            assertThrows(IllegalStateException.class, () -> fade.paint(canvas, window, c -> {
                c.save();
                c.translate(20, 30);
                throw new IllegalStateException("draw failed");
            }));
            assertEquals(before, canvas.getSaveCount());
            assertEquals(0, alpha(surface, 3, 3));
        }
    }

    static int alpha(Surface surface, int x, int y) {
        try (Bitmap pixels = new Bitmap()) {
            pixels.allocN32Pixels(surface.getWidth(), surface.getHeight());
            assertTrue(surface.readPixels(pixels, 0, 0));
            return pixels.getColor(x, y) >>> 24;
        }
    }
}
