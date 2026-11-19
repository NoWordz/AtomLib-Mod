package com.atom.lib.design.atomchat;

/**
 * Legacy process-wide compatibility state and pure colour utilities.
 *
 * <p>Use {@link #defaultHost()} for shipped defaults in a new integration.
 * Existing implicit drawing APIs continue to use the shared compatibility state.
 */
public final class Design {
    private static volatile DesignHost host = Defaults.INSTANCE;

    private Design() {
    }

    /** Optional click-feedback hook: the package draws the card, the host
     *  decides whether the click ripples. */
    public interface RippleSink {
        void region(float x, float y, float width, float height, float radius, int surface);
    }

    private static final RippleSink NO_RIPPLE = (x, y, w, h, r, s) -> { };
    private static volatile RippleSink ripple = NO_RIPPLE;

    /** @deprecated Pass a host-owned sink to a session or the explicit drawing overloads. */
    @Deprecated(since = "0.6.0")
    public static void rippleSink(RippleSink value) {
        ripple = value == null ? NO_RIPPLE : value;
    }

    /** @deprecated Call your own feedback sink or use a session-local feedback layer. */
    @Deprecated(since = "0.6.0")
    public static void ripple(float x, float y, float width, float height, float radius, int surface) {
        ripple.region(x, y, width, height, radius, surface);
    }

    /** @deprecated Create a session or pass your own theme to the explicit token and drawing overloads. */
    @Deprecated(since = "0.6.0")
    public static void host(DesignHost value) {
        host = value == null ? Defaults.INSTANCE : value;
    }

    /** @deprecated Use a session snapshot, your own theme, or {@link #defaultHost()}. */
    @Deprecated(since = "0.6.0")
    public static DesignHost host() {
        return host;
    }
    /** Pure immutable shipped defaults, independent of the legacy global host. */
    public static DesignHost defaultHost() { return Defaults.INSTANCE; }

    /** Whether a colour is light enough to need dark content on top of it. */
    public static boolean isLight(int argb) {
        return relativeLuminance(argb) >= 0.5F;
    }

    public static float relativeLuminance(int argb) {
        float r = srgbToLinear(((argb >>> 16) & 0xFF) / 255.0F);
        float g = srgbToLinear(((argb >>> 8) & 0xFF) / 255.0F);
        float b = srgbToLinear((argb & 0xFF) / 255.0F);
        return 0.2126F * r + 0.7152F * g + 0.0722F * b;
    }

    private static float srgbToLinear(float c) {
        return c <= 0.04045F ? c / 12.92F : (float) Math.pow((c + 0.055F) / 1.055F, 2.4);
    }

    private static final class Defaults implements DesignHost {
        static final Defaults INSTANCE = new Defaults();

        @Override
        public float cornerRadius() {
            return 20.0F;
        }

        @Override
        public boolean halfScreenMode() {
            return false;
        }

        @Override
        public int accentColor() {
            return 0xFF4A90E2;
        }

        @Override
        public int textSecondaryColor() {
            return 0xE8B7BDCA;
        }

        @Override
        public float cardTint() {
            return 0.235F;
        }

        @Override
        public int cardColor() {
            return 0xFFFFFFFF;
        }

        @Override
        public int panelBackgroundColor() {
            return 0xEE16191F;
        }
    }
}
