package com.atom.lib.design.atomchat;

/**
 * Host-supplied state the AtomChat look reads while painting.
 *
 * <p>Pass an immutable snapshot to theme-dependent tokens and painters.
 * {@link Design#defaultHost()} supplies the bundled appearance without
 * consulting the legacy shared state.
 */
public interface DesignHost {
    /** Host-validated corner setting on the 1080p basis, where {@code 20} gives the base radius. */
    float cornerRadius();

    /** True while the panel is attached to a screen edge, which squares off
     *  the outer corners. */
    boolean halfScreenMode();

    int accentColor();

    int textSecondaryColor();

    /** Card fill opacity, {@code 0..1}. */
    float cardTint();

    int cardColor();

    int panelBackgroundColor();
}
