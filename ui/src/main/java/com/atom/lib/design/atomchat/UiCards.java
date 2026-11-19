package com.atom.lib.design.atomchat;

import io.github.humbleui.skija.Canvas;
import io.github.humbleui.skija.FilterTileMode;
import io.github.humbleui.skija.ImageFilter;
import io.github.humbleui.skija.Paint;
import io.github.humbleui.skija.SaveLayerRec;
import io.github.humbleui.types.Rect;

/**
 * Shared card primitive for settings rows, home tiles, category cards and
 * notifications, so their elevation cannot drift between pages:
 *
 * <ul>
 *   <li>a light drop shadow ({@link UiTokens#CARD_SHADOW}, the lower of the
 *       two elevation tiers — chrome floats on {@link UiTokens#CHROME_SHADOW},
 *       content cards sit on this one; blur is s(6), the v0.2.15 lift that
 *       v0.2.16 briefly flattened to s(4) — the tail that spills past
 *       {@link UiTokens#ROW_CLIP_INSET} is its near-transparent fringe),</li>
 *   <li>the configured card fill,</li>
 *   <li>the polarity-adaptive hairline ({@link UiTokens#hairline()}), one
 *       whisper-thin s(1.0) stroke — the three-tier outline ladder was
 *       retired: at its card tier it drew borders heavy enough to read as
 *       frames, not edges; the card's depth is the two shadow tiers plus
 *       this hairline alone,</li>
 *   <li>the hover wash, last.</li>
 * </ul>
 *
 * <p>Disabled rows dim their text and controls only — the card base never
 * changes, so the list keeps one uniform surface instead of patchy light
 * rectangles.</p>
 */
public final class UiCards {
    private UiCards() {
    }

    /** Draws a card with an explicit palette and no click feedback. */
    public static void drawCard(Canvas canvas, float x, float y, float w, float h,
                                float radius, float hoverWeight, DesignHost theme) {
        int fill = UiTokens.cardFill(theme);
        paintCard(canvas, x, y, w, h, radius, hoverWeight,
                UiTokens.CARD_SHADOW, UiTokens.CONTENT_SHADOW_BLUR, fill, theme, outline(theme));
    }

    /** Instance palette path. Never installs or reads global Design state. */
    public static void drawCard(Canvas canvas, float x, float y, float w, float h,
                                float radius, float hoverWeight, DesignHost host, Design.RippleSink ripple) {
        int fill = UiTokens.cardFill(host);
        ripple.region(x, y, w, h, radius, fill);
        paintCard(canvas, x, y, w, h, radius, hoverWeight,
                UiTokens.CARD_SHADOW, UiTokens.CONTENT_SHADOW_BLUR, fill, host, outline(host));
    }

    /** Explicit shadow and face for host-specific surfaces such as notifications. */
    public static void drawCard(Canvas canvas, float x, float y, float w, float h,
                                float radius, float hoverWeight, int shadowColor, float shadowBlur,
                                int baseFill, DesignHost host, Design.RippleSink ripple) {
        ripple.region(x, y, w, h, radius, baseFill);
        paintCard(canvas, x, y, w, h, radius, hoverWeight, shadowColor, shadowBlur, baseFill, host, outline(host));
    }

    private static int outline(DesignHost host) {
        if (host instanceof AtomChatControlStyle style && style.outlineColor() != 0) {
            return style.outlineColor();
        }
        return UiTokens.hairline(host);
    }

    /** Filters only the current rounded backdrop; call before the card's alpha layer. */
    public static void drawNotificationBackdrop(Canvas canvas, float x, float y, float w, float h,
                                                float radius, float alpha) {
        int save = canvas.save();
        try (ImageFilter blur = ImageFilter.makeBlur(UiTokens.NOTIFICATION_BLUR,
                UiTokens.NOTIFICATION_BLUR, FilterTileMode.CLAMP);
             Paint paint = new Paint().setAlphaf(alpha)) {
            SkiaDraw.clip(canvas, x, y, w, h, radius);
            // The filter reads the current layer, including panel content during
            // open/close. No borrowed GPU image survives a frame or context reset.
            canvas.saveLayer(new SaveLayerRec(Rect.makeXYWH(x, y, w, h), paint, blur));
            canvas.restore();
        } finally {
            canvas.restoreToCount(save);
        }
    }

    /** Draws the full card stack; {@code hoverWeight} 0..1. */
    @Deprecated(since = "0.6.0")
    public static void drawCard(Canvas canvas, float x, float y, float w, float h,
                                float radius, float hoverWeight) {
        drawCard(canvas, x, y, w, h, radius, hoverWeight, UiTokens.CARD_SHADOW, UiTokens.CONTENT_SHADOW_BLUR);
    }

    /**
     * Draws the full card stack with an explicit shadow tier; {@code hoverWeight}
     * 0..1. Notifications supply their card fill with a readability floor;
     * action feedback retains its separate {@link #drawFloatSurface} material.
     */
    @Deprecated(since = "0.6.0")
    public static void drawCard(Canvas canvas, float x, float y, float w, float h,
                                float radius, float hoverWeight, int shadowColor, float shadowBlur) {
        drawCard(canvas, x, y, w, h, radius, hoverWeight, shadowColor, shadowBlur,
                UiTokens.cardFill());
    }

    /**
     * The full stack with the base fill explicit too, for a card whose face is
     * not the configured card colour. Content surfaces keep
     * {@link UiTokens#cardFill()} through the overloads above.
     */
    @Deprecated(since = "0.6.0")
    public static void drawCard(Canvas canvas, float x, float y, float w, float h,
                                float radius, float hoverWeight, int shadowColor, float shadowBlur,
                                int baseFill) {
        Design.ripple(x, y, w, h, radius, baseFill);
        DesignHost host = Design.host();
        paintCard(canvas, x, y, w, h, radius, hoverWeight, shadowColor, shadowBlur, baseFill, host, UiTokens.hairline(host));
    }

    private static void paintCard(Canvas canvas, float x, float y, float w, float h,
                                  float radius, float hoverWeight, int shadowColor, float shadowBlur,
                                  int baseFill, DesignHost host, int outline) {
        SkiaDraw.drawRoundedShadow(canvas, x, y, w, h, radius, shadowBlur, shadowColor);
        SkiaDraw.drawRoundedRect(canvas, x, y, w, h, radius, baseFill);
        SkiaDraw.drawEdgeHighlight(canvas, x, y, w, h, radius, UiTokens.s(1.0F), outline);
        if (hoverWeight > 0.01F) {
            SkiaDraw.drawRoundedRect(canvas, x, y, w, h, radius, UiTokens.cardHover(hoverWeight, host));
        }
    }

    /**
     * A surface that floats above message content — the notification banner and
     * the action toast. Same stack as {@link #drawCard}, but on the two-tier
     * float shadow the shell header, composer and tab bar ride too, one third
     * lighter in both passes ({@link UiTokens#FLOAT_SHADOW_INNER}): these float
     * over arbitrary content, so their shadow has to contain the card softly
     * rather than sit under it as a dark second edge — exactly how the single
     * tier they used before read. The hairline and hover wash are resolved
     * against the caller's fill rather than the panel's card colour, so a float
     * whose fill is not the card colour still gets an edge and a wash that match
     * it.
     */
    @Deprecated(since = "0.6.0")
    public static void drawFloatSurface(Canvas canvas, float x, float y, float w, float h,
                                        float radius, float hoverWeight, int baseFill) {
        drawFloatSurface(canvas, x, y, w, h, radius, hoverWeight, baseFill, Design.host());
    }

    public static void drawFloatSurface(Canvas canvas, float x, float y, float w, float h,
                                        float radius, float hoverWeight, int baseFill, DesignHost host) {
        SkiaDraw.drawChromeShadow(canvas, x, y, w, h, radius,
                UiTokens.FLOAT_SHADOW_INNER, UiTokens.FLOAT_SHADOW_OUTER);
        SkiaDraw.drawRoundedRect(canvas, x, y, w, h, radius, baseFill);
        SkiaDraw.drawEdgeHighlight(canvas, x, y, w, h, radius, UiTokens.s(1.0F),
                UiTokens.hairline(baseFill));
        if (hoverWeight > 0.01F) {
            SkiaDraw.drawRoundedRect(canvas, x, y, w, h, radius, UiTokens.cardHover(
                    host.accentColor(), baseFill, hoverWeight));
        }
    }
}
