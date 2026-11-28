package com.atom.lib.design.preview;

import com.atom.lib.geometry.Rect;
import com.atom.lib.layout.NodeId;
import com.atom.lib.layout.PresentationFrame;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Surface;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.atom.lib.design.preview.PreviewPageTransition.Direction.*;
import static com.atom.lib.design.preview.PreviewPageTransition.Style.*;
import static org.junit.jupiter.api.Assertions.*;

class PreviewPageTransitionTest {
    private static final Rect BODY = new Rect(4, 12, 144, 68);
    private static final NodeId CONTROL = new NodeId("card");

    @Test void repeatedInterruptionsPreserveLastDisplayedCompositionAndOnlyArrivingPageRuns() {
        try (var motion = new PreviewPageTransition(); var target = Surface.makeRasterN32Premul(160, 96)) {
            int[] calls = new int[3];
            var a = draw(0xffcd5522, calls, 0);
            var b = draw(0xff338855, calls, 1);
            var c = draw(0xff4343f2, calls, 2);
            paint(motion, target, 1, 0, 0, SLIDE, true, a);
            byte[] original = pixels(target);
            paint(motion, target, 2, 16, 1, SLIDE, true, b);
            assertArrayEquals(original, pixels(target));
            paint(motion, target, 3, 76, 1, SLIDE, true, b);
            byte[] mixed = pixels(target);
            assertFalse(java.util.Arrays.equals(original, mixed));
            var layout = PresentationFrame.hosted(1, CONTROL, new Rect(10, 22, 20, 16));
            assertEquals(10 + 44 * .75f * .75f * .75f,
                    motion.presented(layout).box(CONTROL).hit().x(), 0.001f);
            paint(motion, target, 4, 100, 2, ZOOM, true, c);
            assertArrayEquals(mixed, pixels(target), "Input between paints must freeze the last drawn frame");
            paint(motion, target, 5, 101, 0, SLIDE, true, a);
            assertArrayEquals(mixed, pixels(target), "Two immediate changes cannot blank the outgoing body");
            assertArrayEquals(new int[]{2, 2, 1}, calls);
            paint(motion, target, 6, 341, 0, SLIDE, true, a);
            assertArrayEquals(original, pixels(target));
            assertEquals(layout.box(CONTROL).hit(), motion.presented(layout).box(CONTROL).hit());
        }
    }

    @Test void disabledMotionAndResizingPaintImmediatelyAndClipBelowFixedChrome() {
        try (var motion = new PreviewPageTransition(); var target = Surface.makeRasterN32Premul(160, 96)) {
            int[] calls = new int[2];
            var a = draw(0xffcd5522, calls, 0);
            var b = draw(0xff338855, calls, 1);
            paint(motion, target, 1, 0, 0, SLIDE, true, a);
            paint(motion, target, 2, 20, 1, ZOOM, true, b);
            paint(motion, target, 3, 31, 1, ZOOM, false, b);
            byte[] noMotion = pixels(target);
            paint(motion, target, 4, 70, 1, ZOOM, true, b);
            assertArrayEquals(noMotion, pixels(target));
            target.getCanvas().clear(0);
            motion.paint(target.getCanvas(), new Rect(4, 20, 128, 55), 0, BACK, ZOOM, true,
                    new FrameTime(5, 80_000_000, 0), a);
            assertEquals(0, PreviewAssetsTest.alpha(target, 16, 19));
            assertEquals(255, PreviewAssetsTest.alpha(target, 16, 20));
            assertEquals(0, PreviewAssetsTest.alpha(target, 16, 75));
        }
    }

    @Test void hostClipIsAppliedToDisplayedInputAfterTheBodyTransform() {
        Rect host = new Rect(32, 20, 60, 40);
        var layout = PresentationFrame.hosted(1, CONTROL, BODY);
        try (var motion = new PreviewPageTransition(); var target = Surface.makeRasterN32Premul(160, 96)) {
            var canvas = target.getCanvas();
            int[] calls = new int[1];
            var draw = draw(0xffcd5522, calls, 0);
            motion.paint(canvas, BODY, host, 0, FORWARD, ZOOM, true, new FrameTime(1, 0, 0), draw);
            assertEquals(host, motion.presented(layout).box(CONTROL).clip());
            assertEquals(0, PreviewAssetsTest.alpha(target, 24, 32));
            assertEquals(255, PreviewAssetsTest.alpha(target, 34, 32));
            motion.paint(canvas, BODY, host, 1, FORWARD, ZOOM, true, new FrameTime(2, 16_000_000, 0), draw);
            assertEquals(0, motion.presented(layout).box(CONTROL).clip().width());
            motion.paint(canvas, BODY, host, 1, FORWARD, ZOOM, true, new FrameTime(3, 96_000_000, 0), draw);
            assertEquals(host, motion.presented(layout).box(CONTROL).clip());
        }
    }

    @Test void denseInterruptionCompactsOnTheSameFractionalDeviceGrid() {
        try (var motion = new PreviewPageTransition(); var target = Surface.makeRasterN32Premul(252, 156);
             var paint = new Paint().setColor(0xffde8122)) {
            var canvas = target.getCanvas();
            canvas.translate(.25f, .75f);
            canvas.scale(1.5f, 1.5f);
            Consumer<Canvas> dense = page -> {
                for (int i = 0; i < 13_000; i++) {
                    float x = (i % 128) + 11.25f;
                    float y = ((i / 128) % 58) + 15.125f;
                    page.drawRect(io.github.humbleui.types.Rect.makeXYWH(x, y, .7f, .8f), paint);
                }
            };
            int[] calls = new int[1];
            var simple = draw(0xff4466cc, calls, 0);
            paint(motion, target, 1, 0, 0, SLIDE, true, dense);
            paint(motion, target, 2, 16, 1, ZOOM, true, simple);
            paint(motion, target, 3, 83, 1, ZOOM, true, simple);
            byte[] visible = pixels(target);
            paint(motion, target, 4, 95, 0, SLIDE, true, dense);
            assertArrayEquals(visible, pixels(target));
            paint(motion, target, 5, 335, 0, SLIDE, true, dense);
            assertEquals(0, PreviewAssetsTest.alpha(target, 1, 1));
        }
    }

    private static Consumer<Canvas> draw(int color, int[] calls, int index) {
        return canvas -> {
            calls[index]++;
            try (var paint = new Paint().setColor(color)) {
                canvas.drawRect(io.github.humbleui.types.Rect.makeWH(160, 96), paint);
            }
        };
    }

    private static void paint(PreviewPageTransition motion, Surface surface, long frame, long ms, long page,
                              PreviewPageTransition.Style style, boolean enabled, Consumer<Canvas> body) {
        surface.getCanvas().clear(0);
        motion.paint(surface.getCanvas(), BODY, page, FORWARD, style, enabled, new FrameTime(frame, ms * 1_000_000, 0), body);
    }

    private static byte[] pixels(Surface target) {
        try (var image = target.makeImageSnapshot(); var png = image.encodeToData()) {
            return png.getBytes();
        }
    }
}
