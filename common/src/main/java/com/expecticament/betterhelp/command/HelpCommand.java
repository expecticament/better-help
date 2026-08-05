package com.expecticament.betterhelp.command;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.click.CustomClickActions;
import com.expecticament.betterhelp.text.Components;
import com.expecticament.betterhelp.text.Pagination;
import com.expecticament.betterhelp.metadata.ModCommandMetadata;
import com.expecticament.betterhelp.metadata.ModMetadata;
import com.expecticament.betterhelp.metadata.ModMetadataRegistry;
import com.expecticament.betterhelp.translation.TranslationManager;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.context.StringRange;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.tree.ArgumentCommandNode;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public final class HelpCommand {
    public static final Identifier PAGE_CLICK_ID = Identifier.fromNamespaceAndPath(Constants.MOD_ID, "help_page");
    private static final int PAGE_SIZE = 17;
    private static final int MAX_USAGE_COUNT = 25;

    private static final SimpleCommandExceptionType ERROR_FAILED = new SimpleCommandExceptionType(Component.translatable("commands.help.failed"));

    @SuppressWarnings("unchecked")
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext, Commands.CommandSelection selection) {
        String commandName = "help";

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
            children.remove(commandName);
            literals.remove(commandName);
            arguments.remove(commandName);
        } catch (ReflectiveOperationException e) {
            Constants.LOGGER.error("Failed to unregister vanilla /{} command", commandName, e);
        }

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

    private static SuggestionProvider<CommandSourceStack> suggestCommands(CommandDispatcher<CommandSourceStack> dispatcher) {
        return (context, builder) -> {
            String remaining = builder.getRemaining();
            ParseResults<CommandSourceStack> parse = dispatcher.parse(remaining, context.getSource());

            return dispatcher.getCompletionSuggestions(parse).thenApply(suggestions -> offsetSuggestions(suggestions, builder.getStart()));
        };
    }

    private static Suggestions offsetSuggestions(Suggestions suggestions, int offset) {
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

    private static StringRange offsetRange(StringRange range, int offset) {
        return StringRange.between(range.getStart() + offset, range.getEnd() + offset);
    }

    private static int showPage(CommandSourceStack source, int page) {
        CommandDispatcher<CommandSourceStack> dispatcher = source.getServer().getCommands().getDispatcher();
        List<String> commands = dispatcher.getRoot().getChildren().stream()
                .filter(node -> node.canUse(source))
                .map(CommandNode::getName)
                .sorted(Comparator.naturalOrder())
                .toList();

        Component message = Component.literal("\n").append(Pagination.of(source, commands, page, PAGE_SIZE, (commandName) -> commandComponent(dispatcher, source, commandName), PAGE_CLICK_ID));

        source.sendSystemMessage(message);

        return Command.SINGLE_SUCCESS;
    }

    private static Component commandComponent(CommandDispatcher<CommandSourceStack> dispatcher, CommandSourceStack source, String commandName) {
        CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild(commandName);
        CommandNode<CommandSourceStack> redirect = node.getRedirect();
        ClickEvent clickEvent = new ClickEvent.SuggestCommand("/" + commandName + " ");

        Component commandNameComponent;
        MutableComponent hoverComponent = Component.empty();

        if (redirect == null) {
            commandNameComponent = Component.literal("/" + commandName).setStyle(Constants.STYLE_COMMAND_NAME);
            hoverComponent.append(commandNameComponent);

        } else {
            String redirectName = redirect.getName();
            commandNameComponent = aliasComponent(commandName, redirectName);
            hoverComponent
                    .append(commandNameComponent)
                    .append("\n")
                    .append(Component.literal(TranslationManager.translate(source, "%s.commands.help.alias_of".formatted(Constants.MOD_ID))).setStyle(Constants.STYLE_BODY))
                    .append(Component.literal(" /" + redirectName + ". ").setStyle(Constants.STYLE_BODY));
        }

        String descriptionCommandName = redirect != null ? redirect.getName() : commandName;
        List<String> descriptionPath = List.of(descriptionCommandName);
        ModCommandMetadata modCommandMetadata = ModMetadataRegistry.getCommandMetadata(descriptionCommandName);
        String descriptionText = ModMetadataRegistry.getDescription(modCommandMetadata, source, descriptionPath);
        if (descriptionText != null) {
            hoverComponent
                    .append("\n")
                    .append(Component.literal(descriptionText).setStyle(Constants.STYLE_BODY));
        }

        Map<CommandNode<CommandSourceStack>, String> usages = dispatcher.getSmartUsage(node, source);
        if (!usages.isEmpty()) {
            MutableComponent usagesComponent = Component.empty().setStyle(Constants.STYLE_FADED);
            int count = 1;
            for (String usage : usages.values()) {
                if (count > MAX_USAGE_COUNT) {
                    usagesComponent.append("\n...");
                    break;
                }

                usagesComponent.append(Component.literal("\n/" + commandName + " " + usage));

                count++;
            }

            hoverComponent
                    .append("\n")
                    .append(usagesComponent);
        }

        if (modCommandMetadata != null && ModMetadataRegistry.hasModDescription(modCommandMetadata, source, descriptionPath)) {
            hoverComponent
                    .append("\n\n")
                    .append(Component.literal(Constants.SYMBOL_INFO + " ").setStyle(Constants.STYLE_FADED))
                    .append(Component.literal(TranslationManager.translate(source, "%s.commands.help.from_mod".formatted(Constants.MOD_ID))).setStyle(Constants.STYLE_FADED))
                    .append(" ")
                    .append(Component.literal(modCommandMetadata.getModId()).setStyle(Constants.STYLE_SECONDARY));
        }

        HoverEvent hoverEvent = new HoverEvent.ShowText(hoverComponent);

        return Components.intoBulletedListEntry(commandNameComponent.copy().setStyle(Constants.STYLE_COMMAND_NAME.withClickEvent(clickEvent).withHoverEvent(hoverEvent)));
    }

    private static MutableComponent aliasComponent(String commandName, String redirectName) {
        return Component.empty()
                .append(Component.literal("/" + commandName).setStyle(Constants.STYLE_COMMAND_NAME))
                .append(Component.literal(" -> ").setStyle(Constants.STYLE_FADED))
                .append(Component.literal("/" + redirectName).setStyle(Constants.STYLE_COMMAND_NAME));
    }

    private static int showCommandHelp(CommandSourceStack source, String commandInput) throws CommandSyntaxException {
        CommandDispatcher<CommandSourceStack> dispatcher = source.getServer().getCommands().getDispatcher();

        String input = commandInput.trim();
        if (input.isBlank()) {
            return showPage(source, 1);
        }

        ParseResults<CommandSourceStack> parse = dispatcher.parse(input, source);
        List<ParsedCommandNode<CommandSourceStack>> parsedNodes = parse.getContext().getNodes();
        if (parsedNodes.isEmpty() || !parsedNodes.getFirst().getNode().canUse(source)) {
            throw ERROR_FAILED.create();
        }

        CommandNode<CommandSourceStack> rootNode = parsedNodes.getFirst().getNode();
        CommandNode<CommandSourceStack> redirect = rootNode.getRedirect();
        String parsedPath = parsedCommandPath(input, parsedNodes);

        MutableComponent message = Component.empty().append("\n");
        message.append(Component.literal("/" + parsedPath).setStyle(Constants.STYLE_COMMAND_NAME));

        if (redirect != null) {
            message.append("\n")
                    .append(Component.literal(TranslationManager.translate(source, Constants.MOD_ID + ".commands.help.alias_of")).setStyle(Constants.STYLE_BODY))
                    .append(Component.literal(" /" + redirect.getName() + ".").setStyle(Constants.STYLE_BODY));
        }

        List<String> descriptionPath = descriptionPath(parsedNodes, redirect);
        ModCommandMetadata modCommandMetadata = ModMetadataRegistry.getCommandMetadata(descriptionPath.getFirst());
        String descriptionText = ModMetadataRegistry.getDescription(modCommandMetadata, source, descriptionPath);
        if (descriptionText != null) {
            message
                    .append("\n")
                    .append(Component.literal(descriptionText).setStyle(Constants.STYLE_BODY));
        }

        CommandNode<CommandSourceStack> usageNode = parsedNodes.getLast().getNode();
        if (usageNode.getRedirect() != null) {
            usageNode = usageNode.getRedirect();
        }

        Map<CommandNode<CommandSourceStack>, String> usages = dispatcher.getSmartUsage(usageNode, source);
        if (!usages.isEmpty()) {
            message.append("\n\n")
                    .append(Component.literal(TranslationManager.translate(source, Constants.MOD_ID + ".commands.help.usage")).setStyle(Constants.STYLE_PRIMARY));

            String usagePrefix = "/" + parsedPath;
            int count = 1;
            for (String usage : usages.values()) {
                if (count > MAX_USAGE_COUNT) {
                    message.append(Component.literal("\n...").setStyle(Constants.STYLE_FADED));
                    break;
                }

                message
                        .append("\n")
                        .append(Components.intoBulletedListEntry(Component.literal(usagePrefix + " " + usage)));
                count++;
            }
        }

        if (modCommandMetadata != null && ModMetadataRegistry.hasModDescription(modCommandMetadata, source, descriptionPath)) {
            String modId = modCommandMetadata.getModId();
            ModMetadata metadata = ModMetadataRegistry.getModMetadata(modId);
            String homepage = metadata != null ? metadata.getHomepageUrl() : null;
            message.append("\n\n")
                    .append(Component.literal(Constants.SYMBOL_INFO + " ").setStyle(Constants.STYLE_FADED))
                    .append(Component.literal(TranslationManager.translate(source, Constants.MOD_ID + ".commands.help.from_mod")).setStyle(Constants.STYLE_FADED))
                    .append(" ")
                    .append(homepage != null && !homepage.isBlank()
                            ? Components.link(source, modId, URI.create(homepage), "%s.commands.help.homepage_hover".formatted(Constants.MOD_ID))
                            : Component.literal(modId).setStyle(Constants.STYLE_SECONDARY));
        }

        source.sendSystemMessage(message);

        return Command.SINGLE_SUCCESS;
    }

    private static String parsedCommandPath(String input, List<ParsedCommandNode<CommandSourceStack>> parsedNodes) {
        int end = parsedNodes.getLast().getRange().getEnd();
        return input.substring(0, Math.min(end, input.length())).trim();
    }

    private static List<String> descriptionPath(List<ParsedCommandNode<CommandSourceStack>> parsedNodes, CommandNode<CommandSourceStack> redirect) {
        List<String> segments = new ArrayList<>(parsedNodes.size());
        segments.add(redirect != null ? redirect.getName() : parsedNodes.getFirst().getNode().getName());
        for (int i = 1; i < parsedNodes.size(); i++) {
            segments.add(parsedNodes.get(i).getNode().getName());
        }
        return segments;
    }
}
