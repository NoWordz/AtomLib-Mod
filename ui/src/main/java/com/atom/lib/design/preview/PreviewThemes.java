// SPDX-License-Identifier: MIT
// Copyright (c) 2026 E33EPUS
// Preview palette data adapted from AtomChat 09b23f94, shared/.../theme/ThemeService.java.
// Full license: licenses/AtomChat-MIT.txt.
package com.atom.lib.design.preview;

import com.atom.lib.design.atomdefault.AtomDefaultPalette;
import com.atom.lib.design.atomdefault.ThemeCatalog;

import java.util.List;

/** The optional AtomChat Preview palettes, separate from the AtomDefault catalog. */
public final class PreviewThemes {
    private static final float GLASS_OPACITY = .70f;

    public record Preset(AtomDefaultPalette palette, int capsuleBackground, int capsuleText) {}

    private static final List<Preset> PRESETS = List.of(
            opaque("minimal", 0xff2563d8, 0xffe7edfa, 0xff151a22, 0xff596273, 0xffffffff, 0xff4a6285, 0xffd4dfef, 0xff4e596b),
            opaque("summer", 0xfface246, 0xffffffec, 0xff192521, 0xff56634c, 0xffecf4d4, 0xff466c00, 0xffd8e8b6, 0xff466c00),
            opaque("elegant", 0xffde6e1a, 0xfff5ebdb, 0xff332e23, 0xff685f53, 0xfff3dfc8, 0xff806445, 0xffefd4ae, 0xff615548),
            opaque("raven", 0xffffc233, 0xff1d283a, 0xfff0f4f8, 0xffa4afbd, 0xff263650, 0xff07102a, 0xff3a4b66, 0xffcdd7e5),
            opaque("hub", 0xffff8203, 0xff151414, 0xffffffff, 0xffb0b9c7, 0xff262323, 0xff000000, 0xff423833, 0xffd4d4d4),
            opaque("vivid", 0xffff4d8c, 0xffffebf8, 0xff36212b, 0xff785666, 0xfffad9e9, 0xff8c526b, 0xfff2bfd5, 0xff85304f),
            glass("dusk", 0xffb69dff, 0xff352b48, 0xfff5f6fa, 0xfff5f6fa, 0xff302958, 0xff1d124c, 0xff413960, 0xfff5f6fa),
            glass("mint", 0xff5eead4, 0xff2e7e72, 0xffebfdf7, 0xffebfdf7, 0xff61b8a0, 0xff12392f, 0xff348c78, 0xffebfdf7),
            glass("ice_glass", 0xff246889, 0xffe4f0f5, 0xff152d3b, 0xff314653, 0xfff2f8fb, 0xff536e7d, 0xd9c4dce7, 0xff294d63),
            glass("sand_glass", 0xff926027, 0xfff2e7d6, 0xff352819, 0xff51412e, 0xfffff8eb, 0xff826c50, 0xd9dfccaf, 0xff584025)
    );
    private static final ThemeCatalog CATALOG = new ThemeCatalog(PRESETS.stream().map(Preset::palette).toList());

    private PreviewThemes() {}

    public static ThemeCatalog catalog() {
        return CATALOG;
    }

    public static List<Preset> presets() {
        return PRESETS;
    }

    public static Preset find(String id) {
        return PRESETS.stream().filter(preset -> preset.palette().id().equals(id)).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown Preview theme: " + id));
    }

    private static Preset opaque(String id, int accent, int panel, int text, int secondary,
                                 int card, int outline, int capsule, int capsuleText) {
        return new Preset(new AtomDefaultPalette(id, accent, panel, text, secondary, card, outline, 1f, false),
                capsule, capsuleText);
    }

    private static Preset glass(String id, int accent, int panel, int text, int secondary,
                                int card, int outline, int capsule, int capsuleText) {
        return new Preset(new AtomDefaultPalette(id, accent, panel, text, secondary, card, outline, GLASS_OPACITY, true),
                capsule, capsuleText);
    }
}
