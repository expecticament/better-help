package com.expecticament.betterhelp;

import com.expecticament.betterhelp.click.CustomClickActions;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.player.CustomClickActionEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(Constants.MOD_ID)
public final class BetterHelpNeoForge {

    public BetterHelpNeoForge() {
        BetterHelp.init();

        IEventBus eventBus = NeoForge.EVENT_BUS;
        eventBus.addListener(this::onServerStarting);
        eventBus.addListener(this::onRegisterCommands);
        eventBus.addListener(this::onCustomClickAction);
    }

    private void onServerStarting(ServerStartingEvent event) {
        BetterHelp.onServerStarting(event.getServer());
    }

    private void onRegisterCommands(RegisterCommandsEvent event) {
        BetterHelp.registerCommands(event.getDispatcher(), event.getBuildContext(), event.getCommandSelection());
    }

    private void onCustomClickAction(CustomClickActionEvent event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (CustomClickActions.handle(player, event.getIdentifier(), event.getPayload())) {
            event.setCanceled(true);
        }
    }
}