// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.client.render;

import com.atom.lib.AtomLibRuntime;
import io.github.humbleui.skija.Surface;
import io.github.humbleui.skija.BackendRenderTarget;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.lwjgl.opengl.GL33C.*;

/** Bounded native/GL lifecycle checks; these do not claim a long-running game soak. */
class RenderLifecycleStressTest extends HiddenGlFixture {
    @Test void alternatingTargetsRetainHostPixelsAndReleaseEveryReplacedSurface() {
        var targets = new FramebufferTarget[]{framebuffer(16, 16), framebuffer(24, 24), framebuffer(32, 32)};
        int passes = 0;
        try (var runtime = AtomLibRuntime.open("lifecycle")) {
            for (int owner = 0; owner < 16; owner++) {
                // Test-only wrapper references observe closure; all native drawing stays inside the lease.
                Surface[] previous = {null};
                BackendRenderTarget[] backend = {null};
                try (var bridge = new SkijaRenderBridge(runtime)) {
                    for (int frame = 0; frame < 32; frame++) {
                        var target = targets[frame % targets.length];
                        glBindFramebuffer(GL_FRAMEBUFFER, target.framebuffer());
                        glViewport(2, 3, 9, 11);
                        glDisable(GL_SCISSOR_TEST);
                        glColorMask(true, true, true, true);
                        glClearColor(16/255f, 32/255f, 48/255f, 1);
                        glClear(GL_COLOR_BUFFER_BIT);
                        int color = frame % 2 == 0 ? 0xffcc2244 : 0xff33aa77;
                        bridge.drawFrame(target, (canvas, borrowed) -> {
                            if (previous[0] != null) {
                                assertTrue(previous[0].isClosed());
                                assertTrue(backend[0].isClosed());
                            }
                            previous[0] = borrowed.surface();
                            backend[0] = borrowed.backendRenderTarget();
                            try (var paint = new io.github.humbleui.skija.Paint().setColor(color)) {
                                canvas.drawRect(io.github.humbleui.types.Rect.makeXYWH(0, 0, 4, 4), paint);
                            }
                        });
                        assertEquals(target.framebuffer(), glGetInteger(GL_DRAW_FRAMEBUFFER_BINDING));
                        int[] viewport = new int[4];
                        glGetIntegerv(GL_VIEWPORT, viewport);
                        assertArrayEquals(new int[]{2, 3, 9, 11}, viewport);
                        assertArrayEquals(new int[]{16, 32, 48, 255}, pixel(target, target.width()-2, 2));
                        assertArrayEquals(new int[]{(color>>16)&255, (color>>8)&255, color&255, 255}, pixel(target, 2, target.height()-2));
                        if (frame % 8 == 0) bridge.invalidate();
                        passes++;
                    }
                }
                assertTrue(previous[0].isClosed());
                assertTrue(backend[0].isClosed());
                assertFalse(runtime.isClosed(), "A bridge borrows its runtime");
            }
        }
        assertEquals(512, passes);
        assertEquals(GL_NO_ERROR, glGetError());
        System.out.println("PASS lifecycle: 16 bridge owners, 512 target swaps, replaced surfaces/backends closed");
    }
}
