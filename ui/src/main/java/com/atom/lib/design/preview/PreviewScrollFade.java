// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.design.preview;

import com.atom.lib.geometry.Rect;
import io.github.humbleui.skija.BlendMode;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Shader;

import java.util.Objects;
import java.util.function.Consumer;

/** Opt-in vertical edge fade. Own one per render view and close it on the render thread. */
public final class PreviewScrollFade implements AutoCloseable {
    private final float reach;
    private Paint erase;
    private boolean closed;

    public PreviewScrollFade() {
        this(14);
    }

    /** Reach in the same logical units as the scroll viewport. */
    public PreviewScrollFade(float reach) {
        if (!Float.isFinite(reach) || reach < 0) throw new IllegalArgumentException("Invalid fade reach");
        this.reach = reach;
    }

    public record Window(Rect bounds, float top, float bottom) {
        public Window {
            Objects.requireNonNull(bounds);
            if (!Float.isFinite(top) || !Float.isFinite(bottom)
                    || top < 0 || bottom < 0 || top + bottom > bounds.height()) {
                throw new IllegalArgumentException("Invalid scroll window");
            }
        }
    }

    public Window window(Rect bounds, float scroll, float maxScroll) {
        if (!Float.isFinite(scroll) || !Float.isFinite(maxScroll) || maxScroll < 0) {
            throw new IllegalArgumentException("Invalid scroll position");
        }
        float offset = Math.max(0, Math.min(scroll, maxScroll));
        float depth = Math.min(bounds.height() / 2, reach);
        return new Window(bounds, Math.min(depth, offset), Math.min(depth, maxScroll - offset));
    }

    /**
     * Paints a complete content group. Reuse the same Window for the scene, backdrop mask,
     * and any separately painted feedback. Parent clipping still applies.
     */
    public void paint(Canvas canvas, Window window, Consumer<Canvas> draw) {
        if (closed) throw new IllegalStateException("Edge fade is closed");
        Objects.requireNonNull(draw);
        Rect bounds = window.bounds();
        if (bounds.width() == 0 || bounds.height() == 0) return;
        var clip = io.github.humbleui.types.Rect.makeXYWH(bounds.x(), bounds.y(), bounds.width(), bounds.height());
        int restore = canvas.save();
        try {
            canvas.clipRect(clip);
            if (window.top() == 0 && window.bottom() == 0) {
                draw.accept(canvas);
                return;
            }
            int content = canvas.saveLayer(clip, null);
            int layer = canvas.save();
            try {
                draw.accept(canvas);
            } finally {
                canvas.restoreToCount(layer);
            }
            var mask = eraser();
            if (window.top() > 0) erase(canvas, bounds, bounds.y(), window.top(), mask);
            if (window.bottom() > 0) erase(canvas, bounds, bounds.bottom(), -window.bottom(), mask);
            canvas.restoreToCount(content);
        } finally {
            canvas.restoreToCount(restore);
        }
    }

    private Paint eraser() {
        if (erase == null) {
            try (var shader = Shader.makeLinearGradient(0, 0, 0, 1,
                    new int[]{0xff000000, 0x00000000})) {
                erase = new Paint().setBlendMode(BlendMode.DST_OUT).setShader(shader);
            }
        }
        return erase;
    }

    private static void erase(Canvas canvas, Rect bounds, float edge, float depth, Paint paint) {
        int save = canvas.save();
        try {
            canvas.translate(bounds.x(), edge);
            canvas.scale(bounds.width(), depth);
            canvas.drawRect(io.github.humbleui.types.Rect.makeWH(1, 1), paint);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    @Override public void close() {
        closed = true;
        if (erase != null) {
            erase.close();
            erase = null;
        }
    }
}
