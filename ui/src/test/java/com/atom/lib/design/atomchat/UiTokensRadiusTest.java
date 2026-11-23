package com.atom.lib.design.atomchat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UiTokensRadiusTest {
    @Test
    void honorsTheHostsRange() {
        assertEquals(1F, UiTokens.radiusFactor(20F));
        assertEquals(1.5F, UiTokens.radiusFactor(30F));
        assertEquals(3F, UiTokens.radiusFactor(60F));
    }

    @Test
    void rejectsInvalidRadiusSettings() {
        for (float invalid : new float[]{-1F, Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY}) {
            assertEquals(0F, UiTokens.radiusFactor(invalid));
        }
    }
}
