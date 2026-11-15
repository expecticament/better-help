package com.expecticament.betterhelp.util;

import com.expecticament.betterhelp.Constants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Helpers for working with Brigadier's {@link CommandDispatcher}.
 */
public final class CommandDispatcherUtil {
    private static final List<Field> FIELDS = findFields();

    private CommandDispatcherUtil() {}

    private static @NotNull List<Field> findFields() {
        try {
            List<Field> fields = new ArrayList<>();
            for (String fieldName : List.of("children", "literals", "arguments")) {
                Field field = CommandNode.class.getDeclaredField(fieldName);
                field.setAccessible(true);
                fields.add(field);
            }
            return List.copyOf(fields);
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to access Brigadier CommandNode fields", e);
            return List.of();
        }
    }

    /**
     * Removes a root command from the root node's {@code children}, {@code literals},
     * and {@code arguments} maps via reflection.
     *
     * @param dispatcher the dispatcher whose root should be modified.
     * @param name       the root command name to remove.
     */
    public static void unregisterCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, @NotNull String name) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        try {
            for (Field field : FIELDS) {
                ((Map<?, ?>) field.get(root)).remove(name);
            }
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to unregister /{} command", name, e);
        }
    }

    /**
     * Builds a {@code command} - {@code aliases} map.
     * <p>
     * Forks, self-redirects, commands without aliases, and nodes that
     * the {@code source} can't use are not included.
     *
     * @param dispatcher the dispatcher to scan.
     * @param source     used for {@link CommandNode#canUse} checks.
     * @return an unmodifiable {@code command} - {@code aliases} map.
     */
    public static @NotNull Map<String, List<String>> getAliasesByCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, @NotNull CommandSourceStack source) {
        Map<String, List<String>> map = new HashMap<>();
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();

        for (CommandNode<CommandSourceStack> child : root.getChildren()) {
            if (!(child instanceof LiteralCommandNode<?>) || child.isFork()) {
                continue;
            }

            CommandNode<CommandSourceStack> redirect = child.getRedirect();
            if (redirect == null || redirect == child || root.getChild(redirect.getName()) != redirect || !child.canUse(source)) {
                continue;
            }

            map
                    .computeIfAbsent(redirect.getName(), ignored -> new ArrayList<>())
                    .add(child.getName());
        }

        map.replaceAll((key, value) -> value.stream().sorted().toList());
        return Collections.unmodifiableMap(map);
    }
}
