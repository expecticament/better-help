package com.expecticament.betterhelp.text;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.metadata.ModCommandMetadata;
import com.expecticament.betterhelp.metadata.ModMetadata;
import com.expecticament.betterhelp.metadata.ModMetadataRegistry;
import com.expecticament.betterhelp.translation.TranslationManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class CommandHelpBuilder {
    private final CommandDispatcher<CommandSourceStack> dispatcher;
    private final CommandNode<CommandSourceStack> aliasTargetNode;
    private final CommandNode<CommandSourceStack> usageNode;
    private final CommandSourceStack source;
    private final String commandName;
    private final List<String> aliases;
    private final ModCommandMetadata commandMetadata;
    private final ModMetadata modMetadata;
    private final List<String> pathSegments;
    private final String displayPath;

    private boolean includeDescription;
    private boolean includeAliases;
    private boolean aliasesAsNumber;
    private Map<CommandNode<CommandSourceStack>, String> usage;
    private int usageMaxLines;
    private Component modAttributionComponent;

    public CommandHelpBuilder(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, @NotNull CommandSourceStack source, @NotNull String commandName, @Nullable List<String> aliases, @Nullable List<String> pathSegments, @Nullable String displayPath) {
        CommandNode<CommandSourceStack> root = dispatcher.getRoot();
        CommandNode<CommandSourceStack> node = root.getChild(commandName);
        CommandNode<CommandSourceStack> redirect = node != null ? node.getRedirect() : null;

        this.dispatcher = dispatcher;
        this.aliasTargetNode = redirect != null && redirect != node && !node.isFork() && root.getChild(redirect.getName()) == redirect ? redirect : null;
        this.usageNode = redirect != null && redirect != root ? redirect : node;
        this.source = source;
        this.commandName = commandName;
        this.aliases = aliases != null ? List.copyOf(aliases) : List.of();
        this.commandMetadata = ModMetadataRegistry.getCommandMetadata(getBaseCommandName());
        this.pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
        this.displayPath = "/" + (displayPath != null ? displayPath : commandName);
        this.modMetadata = commandMetadata != null ? ModMetadataRegistry.getModMetadata(commandMetadata.getModId()) : null;
    }

    public @NotNull CommandHelpBuilder withDescription() {
        includeDescription = true;
        return this;
    }

    public @NotNull CommandHelpBuilder withAliases(boolean asNumber) {
        includeAliases = !aliases.isEmpty() && aliasTargetNode == null;
        aliasesAsNumber = asNumber;
        return this;
    }

    public @NotNull CommandHelpBuilder withUsage(int maxLines) {
        CommandNode<CommandSourceStack> node = usageNode;
        for (String segment : pathSegments) {
            if (node == null) {
                break;
            }

            node = node.getChild(segment);
        }
        if (node != null) {
            usage = dispatcher.getSmartUsage(node, source);
        }

        usageMaxLines = maxLines;

        return this;
    }

    public @NotNull CommandHelpBuilder withAttribution() {
        if (modMetadata != null) {
            URI homepageUri = modMetadata.getHomepageUri();
            modAttributionComponent = homepageUri != null
                    ? Components.link(source, modMetadata.getName(), homepageUri, Constants.MOD_ID + ".commands.help.homepage_hover")
                    : Component.literal(modMetadata.getName()).setStyle(Constants.STYLE_SECONDARY);
        }

        return this;
    }

    public @NotNull Component buildAsHover() {
        return build(true);
    }

    public @NotNull Component buildAsBlock() {
        return build(false);
    }

    private @NotNull Component build(boolean asHover) {
        boolean withEvents = !asHover;

        MutableComponent aliasComponent = null;
        MutableComponent commandDetailComponent = Component.empty();
        if (asHover) {
            commandDetailComponent.append(Component.literal(displayPath).setStyle(Constants.STYLE_COMMAND_NAME));
        }
        if (includeDescription) {
            Component descriptionComponent = buildDescription(withEvents);
            if (descriptionComponent != null) {
                commandDetailComponent
                        .append("\n")
                        .append(descriptionComponent);
            }
        }
        if (includeAliases) {
            if (aliasesAsNumber) {
                HoverEvent aliasesHoverEvent = new HoverEvent.ShowText(buildAliasList(false));
                aliasComponent = Component.empty()
                        .append(" ")
                        .append(Component.literal("(+%s)".formatted(aliases.size())).setStyle(Constants.STYLE_ALIAS.withHoverEvent(aliasesHoverEvent)));
            } else {
                commandDetailComponent
                        .append("\n\n")
                        .append(buildAliasList(withEvents));
            }
        }
        if (usage != null && !usage.isEmpty() && usageMaxLines > 0) {
            commandDetailComponent.append("\n");
            if (!asHover) {
                commandDetailComponent
                        .append("\n")
                        .append(Component.literal(TranslationManager.translate(source, Constants.MOD_ID + ".commands.help.usage")).setStyle(Constants.STYLE_PRIMARY));
            }

            Style usageStyle = asHover ? Constants.STYLE_FADED : Constants.STYLE_LIST_ENTRY;
            String usagePrefix = displayPath + " ";
            int usageCount = 1;
            for (Map.Entry<CommandNode<CommandSourceStack>, String> entry : usage.entrySet()) {
                if (usageCount > usageMaxLines) {
                    commandDetailComponent
                            .append("\n")
                            .append(Component.literal("...").setStyle(usageStyle));
                    break;
                }

                Component usageLine = Component.empty()
                        .append(Component.literal(usagePrefix).setStyle(usageStyle))
                        .append(Component.literal(entry.getValue()).setStyle(usageStyle));
                commandDetailComponent
                        .append("\n")
                        .append(asHover ? usageLine : Components.intoBulletedListEntry(usageLine));

                usageCount++;
            }
        }
        if (modAttributionComponent != null) {
            commandDetailComponent
                    .append("\n\n")
                    .append(Component.literal(Constants.SYMBOL_INFO + " ").setStyle(Constants.STYLE_FADED))
                    .append(Component.literal(TranslationManager.translate(source, Constants.MOD_ID + ".commands.help.from_mod")).setStyle(Constants.STYLE_FADED))
                    .append(" ")
                    .append(modAttributionComponent);
        }

        ClickEvent clickEvent = new ClickEvent.SuggestCommand(displayPath + " ");
        HoverEvent hoverEvent = asHover ? new HoverEvent.ShowText(commandDetailComponent) : null;
        MutableComponent component = Component.empty().append(Component.literal(displayPath).setStyle(Constants.STYLE_COMMAND_NAME.withClickEvent(clickEvent).withHoverEvent(hoverEvent)));

        if (aliasComponent != null) {
            component.append(aliasComponent);
        }
        if (!asHover) {
            component.append(commandDetailComponent);
        }

        return component;
    }

    private @Nullable Component buildDescription(boolean withEvents) {
        MutableComponent component = null;
        if (aliasTargetNode != null) {
            Component target = withEvents
                    ? new CommandHelpBuilder(dispatcher, source, aliasTargetNode.getName(), aliases, null, null).withAliases(false).buildAsHover()
                    : commandName(aliasTargetNode.getName());
            component = Component.empty()
                    .append(Component.literal(TranslationManager.translate(source, Constants.MOD_ID + ".commands.help.alias_of") + " "))
                    .append(target)
                    .append(". ");
        }

        String descriptionText = ModMetadataRegistry.getDescription(commandMetadata, source, getFullPath(getBaseCommandName()));
        if (descriptionText != null) {
            if (component == null) {
                component = Component.empty();
            }
            component.append(descriptionText);
        }

        return component != null ? component.setStyle(Constants.STYLE_BODY) : null;
    }

    private @NotNull Component buildAliasList(boolean withEvents) {
        MutableComponent component = Component.empty()
                .append(Component.literal(TranslationManager.translate(source, Constants.MOD_ID + ".commands.help.aliases") + " ").setStyle(Constants.STYLE_ALIAS))
                .setStyle(Constants.STYLE_BODY);

        boolean isFirst = true;
        for (String alias : aliases) {
            if (!isFirst) {
                component.append(", ");
            } else {
                isFirst = false;
            }

            component.append(withEvents
                    ? new CommandHelpBuilder(dispatcher, source, alias, null, null, null).buildAsHover()
                    : commandName(alias));
        }

        return component;
    }

    private static @NotNull Component commandName(@NotNull String commandName) {
        return Component.literal("/" + commandName).setStyle(Constants.STYLE_COMMAND_NAME);
    }

    private @NotNull List<String> getFullPath(@NotNull String commandName) {
        List<String> fullPath = new ArrayList<>();
        fullPath.add(commandName);
        fullPath.addAll(pathSegments);

        return List.copyOf(fullPath);
    }

    private @NotNull String getBaseCommandName() {
        return aliasTargetNode != null ? aliasTargetNode.getName() : commandName;
    }
}
