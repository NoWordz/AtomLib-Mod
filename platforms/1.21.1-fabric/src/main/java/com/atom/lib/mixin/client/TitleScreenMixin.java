// SPDX-License-Identifier: Apache-2.0
package com.atom.lib.mixin.client;

import com.atom.lib.NativePreparation;
import com.atom.lib.client.natives.NativePreparationScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TitleScreen.class)
abstract class TitleScreenMixin extends Screen {
    protected TitleScreenMixin(Text title) { super(title); }

    @Inject(method = "init", at = @At("TAIL"))
    private void atomlib$showNativePreparation(CallbackInfo callback) {
        if (NativePreparation.status().state() != NativePreparation.State.AVAILABLE) {
            addDrawableChild(ButtonWidget.builder(NativePreparationScreen.titleText(), button ->
                    client.setScreen(NativePreparationScreen.whenReady(this, () -> this)))
                    .dimensions(8, 8, 140, 20).build());
        }
    }
}
