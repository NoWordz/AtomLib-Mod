package com.atom.lib.platform.fabric;

import com.atom.lib.layout.internal.YogaNatives;
import com.atom.lib.natives.ClientNatives;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;

public final class AtomLibClient implements ClientModInitializer {
    @Override public void onInitializeClient() {
        ClientNatives.start(FabricLoader.getInstance().getGameDir(), YogaNatives::hasConfiguredNative,
                YogaNatives::usePreparedNative, YogaNatives::initializePreparedNative);
    }
}
