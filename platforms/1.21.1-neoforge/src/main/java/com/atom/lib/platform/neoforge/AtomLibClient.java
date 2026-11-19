package com.atom.lib.platform.neoforge;
import com.atom.lib.NativePreparation;
import com.atom.lib.client.natives.NativePreparationScreen;
import com.atom.lib.client.render.MinecraftRenderBridge;
import com.atom.lib.layout.internal.YogaNatives;
import com.atom.lib.natives.ClientNatives;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.event.GameShuttingDownEvent;
import net.neoforged.fml.loading.FMLPaths;
/**
 * Client-only shutdown wiring. Loaded only through the dist guard in
 * {@link AtomLibMod}, so a dedicated server never touches it.
 */
final class AtomLibClient {
    private AtomLibClient() { }
    static void init() {
        ClientNatives.start(FMLPaths.GAMEDIR.get(), YogaNatives::hasConfiguredNative, YogaNatives::usePreparedNative,
                YogaNatives::initializePreparedNative);
        NeoForge.EVENT_BUS.addListener(AtomLibClient::onTitleScreen);
        // Fired from Minecraft#stop() on the render thread, before the window
        // and its GL context are destroyed. Only an existing bridge is released.
        NeoForge.EVENT_BUS.addListener(AtomLibClient::onShuttingDown);
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
