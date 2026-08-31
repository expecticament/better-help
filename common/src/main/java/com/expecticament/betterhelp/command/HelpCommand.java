package com.expecticament.betterhelp.command;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.click.CustomClickActions;
import com.expecticament.betterhelp.text.CommandHelpBuilder;
import com.expecticament.betterhelp.text.Components;
import com.expecticament.betterhelp.text.Pagination;
import com.expecticament.betterhelp.util.CommandDispatcherUtil;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public final class HelpCommand {
    private static final Identifier PAGE_CLICK_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "help_page");
    private static final int PAGE_SIZE = 17;
    private static final int MAX_USAGE_COUNT = 25;

    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.help.failed"));

    public static void register(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, @NotNull CommandBuildContext buildContext, @NotNull Commands.CommandSelection selection) {
        String commandName = "help";

        CommandDispatcherUtil.unregisterCommand(dispatcher, commandName);

        CustomClickActions.register(PAGE_CLICK_ID, (player, tag) -> showPage(player.createCommandSourceStack(), Pagination.readPage(tag)));

        dispatcher.register(
                Commands.literal(commandName)
                        .executes(ctx -> showPage(ctx.getSource(), 1))
                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                .suggests(suggestCommands(dispatcher))
                                .executes(ctx -> showCommandHelp(ctx.getSource(), StringArgumentType.getString(ctx, "command")))
                        )
        );
    }

    private static @NotNull SuggestionProvider<CommandSourceStack> suggestCommands(@NotNull CommandDispatcher<CommandSourceStack> dispatcher) {
        return (context, builder) -> {
            String remaining = builder.getRemaining();
            ParseResults<CommandSourceStack> parse = dispatcher.parse(remaining, context.getSource());

            return dispatcher.getCompletionSuggestions(parse).thenApply(suggestions -> offsetSuggestions(suggestions, builder.getStart()));
        };
    }

    private static @NotNull Suggestions offsetSuggestions(@NotNull Suggestions suggestions, int offset) {
        if (offset == 0 || suggestions.getList().isEmpty()) {
            return suggestions;
        }

        return new Suggestions(
                offsetRange(suggestions.getRange(), offset),
                suggestions.getList().stream()
                        .map(suggestion -> new Suggestion(offsetRange(suggestion.getRange(), offset), suggestion.getText(), suggestion.getTooltip()))
                        .toList()
        );
    }

    private static @NotNull StringRange offsetRange(@NotNull StringRange range, int offset) {
        return StringRange.between(range.getStart() + offset, range.getEnd() + offset);
    }

    private static int showPage(@NotNull CommandSourceStack source, int page) {
        CommandDispatcher<CommandSourceStack> dispatcher = source.getServer().getCommands().getDispatcher();

        Map<String, List<String>> aliasesByCommand = CommandDispatcherUtil.getAliasesByCommand(dispatcher, source);

        List<String> commands = dispatcher.getRoot().getChildren().stream()
                .filter(node -> node.getRedirect() == null && node.canUse(source))
                .map(CommandNode::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        Component message = Component.literal("\n")
                .append(Pagination.of(
                        source,
                        commands,
                        page,
                        PAGE_SIZE,
                        (commandName) -> Components.intoBulletedListEntry(new CommandHelpBuilder(dispatcher, source, commandName, aliasesByCommand.get(commandName), null, null)
                                .withDescription()
                                .withAliases(true)
                                .withUsage(MAX_USAGE_COUNT)
                                .withAttribution()
                                .buildAsHover()
                        ),
                        PAGE_CLICK_ID)
                );

        source.sendSystemMessage(message);

        return Command.SINGLE_SUCCESS;
    }

    private static int showCommandHelp(@NotNull CommandSourceStack source, @NotNull String commandInput) throws CommandSyntaxException {
        CommandDispatcher<CommandSourceStack> dispatcher = source.getServer().getCommands().getDispatcher();

        String input = commandInput.trim();
        if (input.isBlank()) {
            return showPage(source, 1);
        }

        ParseResults<CommandSourceStack> parse = dispatcher.parse(input, source);
        List<ParsedCommandNode<CommandSourceStack>> parsedNodes = new ArrayList<>();
        for (CommandContextBuilder<CommandSourceStack> current = parse.getContext(); current != null; current = current.getChild()) {
            parsedNodes.addAll(current.getNodes());
        }
        if (parsedNodes.isEmpty() || !parsedNodes.getFirst().getNode().canUse(source)) {
            throw ERROR_FAILED.create();
        }

        CommandNode<CommandSourceStack> rootNode = parsedNodes.getFirst().getNode();
        CommandNode<CommandSourceStack> redirect = rootNode.getRedirect();

        String commandName = rootNode.getName();
        List<String> descriptionPath = descriptionPath(parsedNodes, redirect);
        List<String> pathSegments = descriptionPath.size() > 1 ? descriptionPath.subList(1, descriptionPath.size()) : List.of();

        Map<String, List<String>> aliasesByCommand = CommandDispatcherUtil.getAliasesByCommand(dispatcher, source);

        MutableComponent message = Component.literal("\n").append(
                new CommandHelpBuilder(dispatcher, source, commandName, aliasesByCommand.get(redirect != null ? redirect.getName() : commandName), pathSegments, parsedCommandPath(input, parsedNodes))
                        .withDescription()
                        .withAliases(false)
                        .withUsage(MAX_USAGE_COUNT)
                        .withAttribution()
                        .buildAsBlock()
        );

        source.sendSystemMessage(message);

        return Command.SINGLE_SUCCESS;
    }

    private static @NotNull List<String> descriptionPath(@NotNull List<ParsedCommandNode<CommandSourceStack>> parsedNodes, @Nullable CommandNode<CommandSourceStack> redirect) {
        List<String> segments = new ArrayList<>(parsedNodes.size());
        segments.add(redirect != null ? redirect.getName() : parsedNodes.getFirst().getNode().getName());
        for (int i = 1; i < parsedNodes.size(); i++) {
            segments.add(parsedNodes.get(i).getNode().getName());
        }

        return segments;
    }

    private static @NotNull String parsedCommandPath(@NotNull String input, @NotNull List<ParsedCommandNode<CommandSourceStack>> parsedNodes) {
        int end = parsedNodes.getLast().getRange().getEnd();
        return input.substring(0, Math.min(end, input.length())).trim();
    }
}
