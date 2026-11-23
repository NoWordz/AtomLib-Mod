// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.controls;

import com.atom.lib.geometry.Rect;
import com.atom.lib.text.TextService;
import io.github.humbleui.skija.Canvas;

/** A frame-local chrome snapshot. Business content and hit geometry belong to the collection. */
public interface CollectionSkin {
    void paintItem(Canvas canvas, Rect bounds, boolean grid, boolean hovered);
    void paintHeader(Canvas canvas, Rect bounds);
    void paintHeaderText(Canvas canvas, Rect bounds, String label, TextService text);
    void paintScrollbar(Canvas canvas, Rect bounds, float opacity);

    /** Retains the shipped no-context overload's header and scrollbar appearance. */
    static CollectionSkin defaults() {
        return com.atom.lib.design.atomchat.AtomChatCollectionSkin.defaults();
    }
}
