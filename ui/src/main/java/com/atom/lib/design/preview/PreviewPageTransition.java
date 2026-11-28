// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.design.preview;

import com.atom.lib.geometry.Rect;
import com.atom.lib.layout.PresentationFrame;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.ColorAlphaType;
import io.github.humbleui.skija.Matrix33;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.Picture;
import io.github.humbleui.skija.PictureRecorder;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Opt-in Preview body transition. Paint fixed chrome outside this view; only the arriving
 * body's painter runs. Own and close the view on the render thread.
 */
public final class PreviewPageTransition implements AutoCloseable {
    public enum Direction { FORWARD, BACK }
    public enum Style { SLIDE, ZOOM }

    private static final long DURATION_NANOS = 240_000_000;
    private static final float DISTANCE = 44;
    private static final float ZOOM = .94f;
    private static final int COMPACT_AFTER = 12_000;
    private static final Pose STILL = new Pose(1, 0, 1, 0, 1);

    private record Pose(float opacity, float x, float scale, float oldX, float oldScale) {}

    private Paint alpha;
    private Picture lastBody;
    private Picture departure;
    private Rect bodyBounds;
    private Rect visibleBounds;
    private Matrix33 deviceTransform;
    private Pose shown;
    private long view;
    private long started;
    private float fromX;
    private float fromScale;
    private Style activeStyle;
    private Direction activeDirection;
    private boolean transitioning;
    private boolean closed;

    /**
     * The host selects the view or tab immediately, then supplies its ID, layout and painter.
     * Use one monotonically increasing frame clock. Reset before the render context is replaced.
     */
    public void paint(Canvas canvas, Rect body, long viewId, Direction direction, Style style, boolean enabled,
                      FrameTime time, Consumer<Canvas> arriving) {
        paint(canvas, body, body, viewId, direction, style, enabled, time, arriving);
    }

    /** Supply the effective rectangular host clip in the same coordinates as the body. */
    public void paint(Canvas canvas, Rect body, Rect hostClip, long viewId, Direction direction, Style style,
                      boolean enabled, FrameTime time, Consumer<Canvas> arriving) {
        if (closed) throw new IllegalStateException("Page transition is closed");
        Objects.requireNonNull(arriving);
        Objects.requireNonNull(time);
        Objects.requireNonNull(style);
        Objects.requireNonNull(direction);

        Rect clip = body.intersect(hostClip);
        var transform = canvas.getLocalToDeviceAsMatrix33();
        if (!body.equals(bodyBounds) || !clip.equals(visibleBounds) || !transform.equals(deviceTransform)) reset();
        if (!enabled || clip.width() == 0 || clip.height() == 0) {
            reset();
            withClip(canvas, clip, arriving);
            markShown(body, clip, transform, viewId, STILL);
            return;
        }

        Picture incoming = record(body, arriving);
        try {
            if (lastBody != null && viewId != view) {
                start(canvas, body, time.nowNanos(), direction, style);
            }
            Pose pose = transitioning ? sample(time.nowNanos()) : STILL;
            withClip(canvas, clip, c -> compose(c, body, pose, incoming, departure));
            closePicture(lastBody);
            lastBody = incoming;
            markShown(body, clip, transform, viewId, pose);
            if (pose.opacity() == 1) {
                closePicture(departure);
                departure = null;
                transitioning = false;
            }
        } finally {
            if (lastBody != incoming) incoming.close();
        }
    }

    /** Applies exactly the last painted body's transform and clip for pointer routing. */
    public PresentationFrame presented(PresentationFrame layout) {
        if (shown == null) throw new IllegalStateException("No body has been painted");
        if (shown.opacity() == 0) {
            return layout.clipped(new Rect(bodyBounds.x(), bodyBounds.y(), 0, 0));
        }
        if (shown == STILL) return layout.clipped(visibleBounds);
        return layout.transformed(shown.scale(), centerX(bodyBounds), centerY(bodyBounds), shown.x(), 0)
                .clipped(visibleBounds);
    }

    private void markShown(Rect body, Rect clip, Matrix33 transform, long viewId, Pose pose) {
        bodyBounds = body;
        visibleBounds = clip;
        deviceTransform = transform;
        view = viewId;
        shown = pose;
    }

    private void start(Canvas canvas, Rect body, long now, Direction direction, Style style) {
        if (shown.opacity() == 1) {
            departure = lastBody;
            lastBody = null;
        } else if (shown.opacity() > 0) {
            Picture frozen = record(body, c -> withClip(c, body, layer -> compose(layer, body, shown, lastBody, departure)));
            try {
                if (frozen.getApproximateOpCount() > COMPACT_AFTER) {
                    Picture compacted = compact(canvas, body, frozen);
                    if (compacted != null) {
                        frozen.close();
                        frozen = compacted;
                    }
                }
            } catch (RuntimeException | Error failure) {
                frozen.close();
                throw failure;
            }
            closePicture(departure);
            departure = frozen;
        }

        boolean interrupted = shown.opacity() < 1;
        fromX = interrupted ? shown.x() : style == Style.SLIDE ? sign(direction) * DISTANCE : 0;
        fromScale = interrupted ? shown.scale() : style == Style.ZOOM ? ZOOM : 1;
        activeStyle = style;
        activeDirection = direction;
        started = now;
        transitioning = true;
    }

    private Pose sample(long now) {
        double elapsed = ((double) now - started) / DURATION_NANOS;
        if (elapsed >= 1) return STILL;
        float remaining = 1 - (float) Math.max(0, elapsed);
        remaining = remaining * remaining * remaining;
        float progress = 1 - remaining;
        return new Pose(progress, fromX * remaining, fromScale + (1 - fromScale) * progress,
                activeStyle == Style.SLIDE ? -sign(activeDirection) * DISTANCE * progress : 0,
                activeStyle == Style.ZOOM ? 1 - (1 - ZOOM) * progress : 1);
    }

    private static int sign(Direction direction) {
        return direction == Direction.BACK ? -1 : 1;
    }

    private void compose(Canvas canvas, Rect body, Pose pose, Picture incoming, Picture outgoing) {
        if (pose.opacity() == 1) {
            incoming.playback(canvas);
        } else if (pose.opacity() == 0) {
            outgoing.playback(canvas);
        } else {
            drawLayer(canvas, body, outgoing, 1 - pose.opacity(), pose.oldX(), pose.oldScale());
            drawLayer(canvas, body, incoming, pose.opacity(), pose.x(), pose.scale());
        }
    }

    private void drawLayer(Canvas canvas, Rect body, Picture picture, float opacity, float dx, float zoom) {
        if (alpha == null) alpha = new Paint();
        int save = canvas.saveLayer(skRect(body), alpha.setAlphaf(opacity));
        try {
            float x = centerX(body), y = centerY(body);
            canvas.translate(x + dx, y);
            canvas.scale(zoom, zoom);
            canvas.translate(-x, -y);
            picture.playback(canvas);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    private static float centerX(Rect bounds) { return bounds.x() + bounds.width() / 2; }
    private static float centerY(Rect bounds) { return bounds.y() + bounds.height() / 2; }

    private static io.github.humbleui.types.Rect skRect(Rect bounds) {
        return io.github.humbleui.types.Rect.makeXYWH(bounds.x(), bounds.y(), bounds.width(), bounds.height());
    }

    private static Picture record(Rect body, Consumer<Canvas> draw) {
        try (var recorder = new PictureRecorder()) {
            draw.accept(recorder.beginRecording(skRect(body)));
            return recorder.finishRecordingAsPicture();
        }
    }

    private static void withClip(Canvas canvas, Rect bounds, Consumer<Canvas> draw) {
        int save = canvas.save();
        try {
            canvas.clipRect(skRect(bounds));
            draw.accept(canvas);
        } finally {
            canvas.restoreToCount(save);
        }
    }

    /** Rare fallback for a long chain of interruptions. Keep the current device pixel grid and backend. */
    private static Picture compact(Canvas canvas, Rect body, Picture picture) {
        float[] m = canvas.getLocalToDeviceAsMatrix33().getMat();
        if (m[0] <= 0 || m[4] <= 0 || m[1] != 0 || m[3] != 0 || m[6] != 0 || m[7] != 0 || m[8] != 1) {
            return null;
        }
        float left = (float) Math.floor(body.x() * m[0] + m[2]);
        float top = (float) Math.floor(body.y() * m[4] + m[5]);
        int width = (int) (Math.ceil(body.right() * m[0] + m[2]) - left);
        int height = (int) (Math.ceil(body.bottom() * m[4] + m[5]) - top);
        if (width <= 0 || height <= 0 || (long) width * height > 16_777_216) return null;
        try (var parent = canvas.getSurface()) {
            if (parent == null) return null;
            var info = parent.getImageInfo().withWidthHeight(width, height).withColorAlphaType(ColorAlphaType.PREMUL);
            try (var surface = compatibleResource(() -> parent.makeSurface(info))) {
                if (surface == null) return null;
                var target = surface.getCanvas();
                target.clear(0);
                target.setMatrix(new Matrix33(m[0], 0, m[2] - left, 0, m[4], m[5] - top, 0, 0, 1));
                picture.playback(target);
                try (var image = compatibleResource(surface::makeImageSnapshot)) {
                    if (image == null) return null;
                    // Keep the image rectangle on the same physical pixel boundaries, including fractional UI scales.
                    var destination = io.github.humbleui.types.Rect.makeXYWH((left - m[2]) / m[0],
                            (top - m[5]) / m[4], width / m[0], height / m[4]);
                    return record(body, c -> c.drawImageRect(image, destination));
                }
            }
        }
    }

    private static <T> T compatibleResource(Supplier<T> create) {
        try {
            return create.get();
        } catch (RuntimeException failure) {
            // Skija 0.143.18 constructs a wrapper even if the backend returns nullptr.
            if (failure.getClass() == RuntimeException.class && "Can't wrap nullptr".equals(failure.getMessage())) {
                return null;
            }
            throw failure;
        }
    }

    public void reset() {
        closePicture(lastBody);
        closePicture(departure);
        lastBody = departure = null;
        bodyBounds = visibleBounds = null;
        deviceTransform = null;
        shown = null;
        transitioning = false;
    }

    private static void closePicture(Picture picture) {
        if (picture != null) picture.close();
    }

    @Override public void close() {
        if (!closed) {
            reset();
            if (alpha != null) alpha.close();
            closed = true;
        }
    }
}
