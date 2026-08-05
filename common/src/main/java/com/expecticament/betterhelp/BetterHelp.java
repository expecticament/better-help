package com.expecticament.betterhelp;

import com.expecticament.betterhelp.command.HelpCommand;
import com.expecticament.betterhelp.metadata.ModMetadataRegistry;
import com.expecticament.betterhelp.translation.TranslationManager;
import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;

public final class BetterHelp {
    public static void init() {
        TranslationManager.init();
    }

    public static void onServerStarting(MinecraftServer server) {
        ModMetadataRegistry.load();
    }

    public static void registerCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        HelpCommand.register(dispatcher, buildContext, selection);
    }
}