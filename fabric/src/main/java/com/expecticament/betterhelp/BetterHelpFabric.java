package com.expecticament.betterhelp;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;

public final class BetterHelpFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        BetterHelp.init();

        ServerLifecycleEvents.SERVER_STARTING.register(BetterHelp::onServerStarting);
        CommandRegistrationCallback.EVENT.register(BetterHelp::registerCommands);
    }
}
