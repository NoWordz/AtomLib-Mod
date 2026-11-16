// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.design.atomdefault;

import com.atom.lib.design.atomchat.AtomChatControlStyle;
import java.util.Objects;

/** Whole-frame semantic colours and material. Geometry remains a separate host choice. */
public record ThemeSnapshot(long revision,AtomDefaultPalette palette,float cornerRadius,boolean halfScreen,float cardTint) {
    public ThemeSnapshot {Objects.requireNonNull(palette);if(!Float.isFinite(cornerRadius)||cornerRadius<0||!Float.isFinite(cardTint)||cardTint<0||cardTint>1)throw new IllegalArgumentException("Invalid theme geometry");}
    public AtomChatControlStyle controls(){return new AtomChatControlStyle(cornerRadius,halfScreen,palette.accentColor(),palette.textSecondaryColor(),cardTint,palette.cardColor(),panelColor(false),palette.textColor(),palette.outlineColor());}
    public int inputSurface(){return palette.cardColor();}
    public int inputText(){return palette.textColor();}
    public int selection(){return (palette.accentColor()&0xffffff)|0x55000000;}
    public int cursor(){return palette.accentColor();}
    public int focus(){return palette.accentColor();}
    public int error(){return 0xffe05263;}
    public int disabled(){return (palette.textSecondaryColor()&0xffffff)|0x77000000;}
    /** Apply material opacity once, replacing source alpha. Missing blur always uses an opaque surface. */
    public int panelColor(boolean backdropAvailable){return (palette.panelBackgroundColor()&0xffffff)|((palette.blurEnabled()&&!backdropAvailable?255:Math.round(palette.panelOpacity()*255))<<24);}
}
