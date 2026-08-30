package com.expecticament.betterhelp.util;

import com.expecticament.betterhelp.Constants;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.*;

public final class CommandDispatcherUtil {

    @SuppressWarnings("unchecked")
    public static void unregisterCommand(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, @NotNull String name) {
        try {
            CommandNode<CommandSourceStack> root = dispatcher.getRoot();

            Field childrenField = CommandNode.class.getDeclaredField("children");
            Field literalsField = CommandNode.class.getDeclaredField("literals");
            Field argumentsField = CommandNode.class.getDeclaredField("arguments");
            childrenField.setAccessible(true);
            literalsField.setAccessible(true);
            argumentsField.setAccessible(true);

            Map<String, CommandNode<CommandSourceStack>> children = (Map<String, CommandNode<CommandSourceStack>>) childrenField.get(root);
            Map<String, LiteralCommandNode<CommandSourceStack>> literals = (Map<String, LiteralCommandNode<CommandSourceStack>>) literalsField.get(root);
            Map<String, ArgumentCommandNode<CommandSourceStack, ?>> arguments = (Map<String, ArgumentCommandNode<CommandSourceStack, ?>>) argumentsField.get(root);
            children.remove(name);
            literals.remove(name);
            arguments.remove(name);
        } catch (ReflectiveOperationException e) {
            Constants.LOGGER.error("Failed to unregister /{} command", name, e);
        }
    }

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

        Map<String, List<String>> copy = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : map.entrySet()) {
            copy.put(entry.getKey(), List.copyOf(entry.getValue()));
        }

        return Collections.unmodifiableMap(copy);
    }
}
