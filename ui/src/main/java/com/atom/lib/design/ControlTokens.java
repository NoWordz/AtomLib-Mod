// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.design;

/** Shared logical-unit conversions and default collection geometry, independent of a host theme. */
public final class ControlTokens {
    public static final float SCALE = 1.25F;
    public static final float REFERENCE_CORNER_RADIUS = 20F;
    public static final float COLLECTION_CARD_RADIUS = 8F;
    public static final float COLLECTION_HEADER_FONT_SIZE = 14F;
    public static final float COLLECTION_HEADER_PADDING = 6F;
    public static final float COLLECTION_SCROLLBAR_RADIUS = 2F;

    private ControlTokens() {}

    public static float scale(float value) { return value * SCALE; }

    /** Hosts own the accepted setting range; invalid radii represent square corners. */
    public static float radiusFactor(float radius) {
        return Float.isFinite(radius) && radius > 0F ? radius / REFERENCE_CORNER_RADIUS : 0F;
    }
}
