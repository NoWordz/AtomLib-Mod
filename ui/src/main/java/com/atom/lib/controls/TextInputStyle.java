package com.atom.lib.controls;

import com.atom.lib.font.FontId;
import java.util.Objects;

/** Explicit fallback for custom skins; AtomChat skins supply current instance colours automatically. */
public record TextInputStyle(FontId font,float fontSize,float padding,float singleLineHeight,float multilineHeight,
                             float radius,int background,int foreground,int muted,int accent,int error) {
    public TextInputStyle {
        Objects.requireNonNull(font);
        if(!Float.isFinite(fontSize)||fontSize<=0||!Float.isFinite(padding)||padding<0
            ||!Float.isFinite(singleLineHeight)||singleLineHeight<=2*padding
            ||!Float.isFinite(multilineHeight)||multilineHeight<=2*padding||!Float.isFinite(radius)||radius<0)
            throw new IllegalArgumentException("Invalid input style");
    }
    public static TextInputStyle defaults(){return new TextInputStyle(new FontId("atomlib/default"),14,8,38,120,8,0xff242833,0xfff3f4f6,0xffa0a6b3,0xff6ea8fe,0xffff6677);}
}
