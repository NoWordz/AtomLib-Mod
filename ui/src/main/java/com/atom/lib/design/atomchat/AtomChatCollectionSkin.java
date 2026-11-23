// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.design.atomchat;

import com.atom.lib.controls.CollectionSkin;
import com.atom.lib.design.ControlTokens;
import com.atom.lib.geometry.Rect;
import com.atom.lib.layout.Constraints;
import com.atom.lib.text.TextRequest;
import com.atom.lib.text.TextService;
import io.github.humbleui.skija.Canvas;
import java.util.Objects;

/** Immutable adapter for the existing AtomChat collection chrome. */
public final class AtomChatCollectionSkin implements CollectionSkin {
    private static final CollectionSkin DEFAULT = new AtomChatCollectionSkin(AtomChatControlStyle.defaults());
    private final AtomChatControlStyle style;

    public AtomChatCollectionSkin(AtomChatControlStyle style) { this.style = Objects.requireNonNull(style); }
    public static CollectionSkin defaults() { return DEFAULT; }

    @Override public void paintItem(Canvas canvas, Rect bounds, boolean grid, boolean hovered) {
        UiCards.drawCard(canvas, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                grid ? ControlTokens.COLLECTION_CARD_RADIUS : 0, hovered ? 1 : 0, style, (x,y,w,h,r,s) -> {});
    }

    @Override public void paintHeader(Canvas canvas, Rect bounds) {
        SkiaDraw.drawRoundedRect(canvas, bounds.x(), bounds.y(), bounds.width(), bounds.height(), 0,
                style.panelBackgroundColor() | 0xff000000);
    }

    @Override public void paintHeaderText(Canvas canvas, Rect bounds, String label, TextService text) {
        float padding = ControlTokens.COLLECTION_HEADER_PADDING;
        var layout = text.layout(new TextRequest(label, AtomChatFonts.HEADING,
                        ControlTokens.COLLECTION_HEADER_FONT_SIZE, style.textColor(), 1, true),
                new Constraints(Constraints.Axis.atMost(Math.max(0, bounds.width()-2*padding)), Constraints.Axis.atMost(bounds.height())));
        text.draw(canvas, layout, bounds.x()+padding, bounds.y()+(bounds.height()-layout.size().height())/2);
    }

    @Override public void paintScrollbar(Canvas canvas, Rect bounds, float opacity) {
        SkiaDraw.drawRoundedRect(canvas, bounds.x(), bounds.y(), bounds.width(), bounds.height(),
                ControlTokens.COLLECTION_SCROLLBAR_RADIUS, (Math.round(opacity*130)<<24) | (style.textSecondaryColor()&0xffffff));
    }
}
