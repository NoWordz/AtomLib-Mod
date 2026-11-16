// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.design.atomdefault;
import java.util.*;
/** Immutable data only. Enumeration never loads fonts, Minecraft or a native context. */
public final class ThemeCatalog {
    private final List<AtomDefaultPalette> presets;
    public ThemeCatalog(List<AtomDefaultPalette> presets){this.presets=List.copyOf(presets);if(presets.isEmpty()||new HashSet<>(presets.stream().map(AtomDefaultPalette::id).toList()).size()!=presets.size())throw new IllegalArgumentException("Empty/duplicate theme catalog");}
    public List<AtomDefaultPalette> presets(){return presets;}
    public AtomDefaultPalette find(String id){return presets.stream().filter(p->p.id().equals(id)).findFirst().orElseThrow(()->new IllegalArgumentException("Unknown theme: "+id));}
    public ThemeCatalog with(AtomDefaultPalette palette){Objects.requireNonNull(palette);var copy=new ArrayList<>(presets);copy.removeIf(p->p.id().equals(palette.id()));copy.add(palette);return new ThemeCatalog(copy);}
}
