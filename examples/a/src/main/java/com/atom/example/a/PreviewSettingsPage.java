// SPDX-License-Identifier: Apache-2.0
package com.atom.example.a;

import com.atom.lib.design.atomchat.SkiaDraw;
import com.atom.lib.design.atomdefault.ThemeSnapshot;
import com.atom.lib.design.preview.PreviewPageTransition;
import com.atom.lib.design.preview.PreviewScrollFade;
import com.atom.lib.design.preview.PreviewThemes;
import com.atom.lib.geometry.Rect;
import com.atom.lib.layout.NodeId;
import com.atom.lib.layout.PresentationFrame;
import com.atom.lib.motion.FrameTime;
import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.Font;
import io.github.humbleui.skija.Image;
import io.github.humbleui.skija.Paint;

import java.util.List;

/** Optional Example A settings body with independently selected themes and motion. */
public final class PreviewSettingsPage implements AutoCloseable {
    private static final int ROWS = 6;
    private static final float ROW_HEIGHT = 49;
    private static final float ROW_GAP = 7;
    private static final float RADIUS = 12;
    private static final NodeId SCROLL_BODY = new NodeId("example.a.preview.scroll_body");
    private static final NodeId FIRST_ACTION = new NodeId("example.a.preview.first_action");

    private record Label(String english, String chinese) {
        String text(boolean zh) { return zh ? chinese : english; }
    }

    private static final List<Label> THEME_NAMES = List.of(
            new Label("Minimal", "极简"), new Label("Lime", "青柠"), new Label("Elegant", "典雅"),
            new Label("Raven", "黑鸦"), new Label("Hub", "枢纽"), new Label("Vivid", "活力"),
            new Label("Dusk", "薄暮"), new Label("Mint", "薄荷"), new Label("Ice glass", "冰晶"),
            new Label("Sand glass", "砂金"));
    private static final List<Label> APPEARANCE = List.of(
            new Label("Theme", "主题"), new Label("Panel color", "面板色"), new Label("Card color", "卡片色"),
            new Label("Capsule color", "胶囊色"), new Label("Outline color", "描边色"), new Label("Accent", "强调色"));
    private static final List<Label> MOTION = List.of(
            new Label("Page transition", "切页动效"), new Label("Duration", "时长"),
            new Label("Easing", "缓动"), new Label("Fixed header", "固定上栏"),
            new Label("Interruptible", "连续切换"), new Label("Scroll edges", "滚动边缘"));

    private final PreviewPageTransition transition = new PreviewPageTransition();
    private final PreviewScrollFade fade = new PreviewScrollFade();
    private final float[] scroll = new float[2];
    private int theme = 1, tab;
    private PreviewPageTransition.Direction direction = PreviewPageTransition.Direction.FORWARD;
    private PreviewPageTransition.Style style = PreviewPageTransition.Style.SLIDE;
    private boolean motionEnabled = true;
    private Layout shown;
    private int shownTab;
    private Rect shownAction;
    private Rect shownActionClip;
    private Rect shownScroll;

    /** Panel, body clip and input targets come from this one layout. */
    private record Layout(Rect panel, Rect appearance, Rect motion, Rect body) {
        static Layout measure(Rect panel) {
            float contentWidth = Math.max(0, panel.width() - 24);
            float buttonWidth = Math.min(116, Math.max(0, (contentWidth - 6) / 2));
            float x = panel.x() + 12, y = panel.y() + 12;
            return new Layout(panel, new Rect(x, y, buttonWidth, 28), new Rect(x + buttonWidth + 6, y, buttonWidth, 28),
                    new Rect(x, y + 36, contentWidth, Math.max(0, panel.height() - 60)));
        }
        float maxScroll() { return Math.max(0, ROWS * (ROW_HEIGHT + ROW_GAP) - ROW_GAP - body.height()); }
        Rect row(int index, float scroll) {
            return new Rect(body.x(), body.y() + index * (ROW_HEIGHT + ROW_GAP) - scroll, body.width(), ROW_HEIGHT);
        }
        Rect firstAction(float scroll) {
            Rect row = row(0, scroll);
            float width = Math.min(128, row.width() / 2);
            return new Rect(row.right() - width - 10, row.y() + 11, width, 27);
        }
    }

    /**
     * Paints the same page used by the Example A native preview task. The optional Image
     * is a host-owned snapshot of the background below the panel.
     */
    public void paint(Canvas canvas, Rect panel, Font hostFont, Image backdrop, boolean chinese, FrameTime frame) {
        Layout layout = Layout.measure(panel);
        var preset = PreviewThemes.presets().get(theme);
        var palette = preset.palette();
        var snapshot = new ThemeSnapshot(frame.frameId(), palette, RADIUS, false, 1);
        float offset = scroll[tab] = clampScroll(scroll[tab], layout);
        boolean glass = palette.blurEnabled() && backdrop != null;
        try (Paint paint = new Paint().setAntiAlias(true)) {
            if (glass) SkiaDraw.drawBlurredBackground(canvas, backdrop, panel.x(), panel.y(),
                    panel.width(), panel.height(), RADIUS, 14);
            round(canvas, panel, RADIUS, snapshot.panelColor(glass), paint);
            border(canvas, panel, RADIUS, palette.outlineColor(), paint);
            var window = fade.window(layout.body(), offset, layout.maxScroll());
            transition.paint(canvas, layout.body(), tab, direction, style, motionEnabled, frame,
                    body -> fade.paint(body, window, layer -> rows(layer, layout, preset, hostFont, paint, offset, chinese)));
            tab(canvas, layout.appearance(), 0, new Label("Appearance", "外观").text(chinese), preset, hostFont, paint);
            tab(canvas, layout.motion(), 1, new Label("Motion", "动效").text(chinese), preset, hostFont, paint);
        }
        var hit = transition.presented(PresentationFrame.hosted(frame.frameId(), FIRST_ACTION, layout.firstAction(offset)))
                .box(FIRST_ACTION);
        shown = layout;
        shownTab = tab;
        shownAction = hit.hit();
        shownActionClip = hit.clip();
        var bodyHit = transition.presented(PresentationFrame.hosted(frame.frameId(), SCROLL_BODY, layout.body()))
                .box(SCROLL_BODY);
        shownScroll = bodyHit.hit().intersect(bodyHit.clip());
    }

    private void rows(Canvas canvas, Layout layout, PreviewThemes.Preset preset, Font font, Paint paint,
                      float offset, boolean chinese) {
        var palette = preset.palette();
        var names = tab == 0 ? APPEARANCE : MOTION;
        for (int i = 0; i < ROWS; i++) {
            Rect row = layout.row(i, offset);
            if (row.bottom() <= layout.body().y() || row.y() >= layout.body().bottom()) continue;
            round(canvas, row, RADIUS, palette.cardColor(), paint);
            text(canvas, names.get(i).text(chinese), row.x() + 12, row, palette.textColor(), font, paint);
            if (i == 0) {
                String action = tab == 0 ? THEME_NAMES.get(theme).text(chinese)
                        : switch (style) {
                            case SLIDE -> motionEnabled ? new Label("Slide", "平移").text(chinese)
                                    : new Label("Off", "关闭").text(chinese);
                            case ZOOM -> new Label("Zoom", "缩放").text(chinese);
                        };
                Rect bounds = layout.firstAction(offset);
                round(canvas, bounds, 12, preset.capsuleBackground(), paint);
                text(canvas, action, bounds.x() + 10, bounds, preset.capsuleText(), font, paint);
            } else if (tab == 0) {
                int color = switch (i) {
                    case 1 -> palette.panelBackgroundColor();
                    case 2 -> palette.cardColor();
                    case 3 -> preset.capsuleBackground();
                    case 4 -> palette.outlineColor();
                    default -> palette.accentColor();
                };
                Rect swatch = new Rect(row.right() - 34, row.y() + 13, 22, 22);
                round(canvas, swatch, 6, color, paint);
                border(canvas, swatch, 6, palette.outlineColor(), paint);
            } else {
                Label value = switch (i) {
                    case 1 -> new Label("240 ms", "240 毫秒");
                    case 2 -> new Label("Ease-out cubic", "三次缓出");
                    case 3, 4 -> new Label("On", "开启");
                    default -> new Label("Fade", "渐隐");
                };
                text(canvas, value.text(chinese), row.right() - 118, row, palette.textSecondaryColor(), font, paint);
            }
        }
    }

    private void tab(Canvas canvas, Rect bounds, int index, String label, PreviewThemes.Preset preset, Font font, Paint paint) {
        round(canvas, bounds, 12, preset.capsuleBackground(), paint);
        if (index == tab) border(canvas, bounds, 12, preset.palette().accentColor(), paint);
        text(canvas, label, bounds.x() + 10, bounds, preset.capsuleText(), font, paint);
    }

    private static void round(Canvas c, Rect r, float radius, int color, Paint paint) {
        c.drawRRect(rounded(r, radius), paint.setColor(color));
    }

    private static void border(Canvas c, Rect r, float radius, int color, Paint paint) {
        paint.setMode(io.github.humbleui.skija.PaintMode.STROKE).setStrokeWidth(1).setColor(color);
        try { c.drawRRect(rounded(r.inset(.5f), radius), paint); }
        finally { paint.setMode(io.github.humbleui.skija.PaintMode.FILL); }
    }

    private static io.github.humbleui.types.RRect rounded(Rect r, float radius) {
        return io.github.humbleui.types.RRect.makeXYWH(r.x(), r.y(), r.width(), r.height(), radius);
    }

    private static void text(Canvas c, String value, float x, Rect r, int color, Font font, Paint paint) {
        var metrics = font.getMetrics();
        c.drawString(value, x, r.y() + (r.height() - metrics.getDescent() - metrics.getAscent()) / 2,
                font, paint.setColor(color));
    }

    /** Pointer releases activate only the displayed tab or the arriving page's visible action. */
    public boolean click(float x, float y) {
        if (shown == null) return false;
        if (shown.appearance().contains(x, y)) return selectTab(0);
        if (shown.motion().contains(x, y)) return selectTab(1);
        if (shownTab != tab || !shownAction.contains(x, y) || !shownActionClip.contains(x, y)) return false;
        if (tab == 0) theme = (theme + 1) % PreviewThemes.presets().size();
        else if (!motionEnabled) motionEnabled = true;
        else if (style == PreviewPageTransition.Style.SLIDE) style = PreviewPageTransition.Style.ZOOM;
        else {
            style = PreviewPageTransition.Style.SLIDE;
            motionEnabled = false;
        }
        return true;
    }

    private boolean selectTab(int next) {
        if (next != tab) {
            direction = next > tab ? PreviewPageTransition.Direction.FORWARD : PreviewPageTransition.Direction.BACK;
            tab = next;
        }
        return true;
    }

    public boolean wheel(float x, float y, float distance) {
        if (shown == null || shownTab != tab || !shownScroll.contains(x, y) || !Float.isFinite(distance)) {
            return false;
        }
        scroll[tab] = clampScroll(scroll[tab] + distance, shown);
        return true;
    }

    private static float clampScroll(float offset, Layout layout) {
        return Math.max(0, Math.min(layout.maxScroll(), offset));
    }

    @Override public void close() {
        try { transition.close(); }
        finally { fade.close(); }
    }
}
