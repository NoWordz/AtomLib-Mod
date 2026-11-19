package com.atom.lib.platform.forge;
import com.atom.lib.NativePreparation;
import com.atom.lib.client.natives.NativePreparationScreen;
import com.atom.lib.client.render.MinecraftRenderBridge;
import com.atom.lib.layout.internal.YogaNatives;
import com.atom.lib.natives.ClientNatives;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.GameShuttingDownEvent;
import net.minecraftforge.fml.loading.FMLPaths;
/**
 * Client-only shutdown wiring. Loaded only through the dist guard in
 * {@link AtomLibMod}, so a dedicated server never touches it.
 */
final class AtomLibClient {
    private AtomLibClient() { }
    static void init() {
        ClientNatives.start(FMLPaths.GAMEDIR.get(), YogaNatives::hasConfiguredNative, YogaNatives::usePreparedNative,
                YogaNatives::initializePreparedNative);
        MinecraftForge.EVENT_BUS.addListener(AtomLibClient::onTitleScreen);
        // Fired from Minecraft#stop() on the render thread, before the window
        // and its GL context are destroyed. Only an existing bridge is released.
        MinecraftForge.EVENT_BUS.addListener(AtomLibClient::onShuttingDown);
    }
    private static void onTitleScreen(ScreenEvent.Init.Post event) {
        if (event.getScreen() instanceof TitleScreen title
                && NativePreparation.status().state() != NativePreparation.State.AVAILABLE) {
            event.addListener(Button.builder(Component.translatable("atomlib.natives.title"), button ->
                    Minecraft.getInstance().setScreen(NativePreparationScreen.whenReady(title, () -> title)))
                    .bounds(8, 8, 140, 20).build());
        }
    }
    private static void onShuttingDown(GameShuttingDownEvent event) {
        MinecraftRenderBridge.releaseIfOpen();
    }
}
