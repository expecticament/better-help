package com.expecticament.betterhelp.click;

import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public final class CustomClickActions {
    private static final Map<Identifier, BiConsumer<ServerPlayer, Tag>> HANDLERS = new HashMap<>();

    public static void register(@NotNull Identifier id, @NotNull BiConsumer<ServerPlayer, Tag> handler) {
        HANDLERS.put(id, handler);
    }

    public static boolean handle(@NotNull ServerPlayer player, @NotNull Identifier id, @Nullable Tag tag) {
        BiConsumer<ServerPlayer, Tag> handler = HANDLERS.get(id);
        if (handler == null) {
            return false;
        }

        handler.accept(player, tag);

        return true;
    }
}
