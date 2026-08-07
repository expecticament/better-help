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
    private final @NotNull CommandDispatcher<CommandSourceStack> dispatcher;
    private final @Nullable CommandNode<CommandSourceStack> commandNode;
    private final @Nullable CommandNode<CommandSourceStack> redirectNode;
    private final @NotNull CommandSourceStack source;
    private final @NotNull String commandName;
    private final @NotNull List<String> aliases;
    private final @Nullable ModCommandMetadata commandMetadata;
    private final @Nullable ModMetadata modMetadata;
    private final @NotNull List<String> pathSegments;
    private final @NotNull String displayPath;

    private @Nullable MutableComponent descriptionComponent;
    private @Nullable Map<CommandNode<CommandSourceStack>, String> usage;
    private int usageMaxLines = 0;
    private @Nullable Component aliasListComponent;
    private boolean aliasesAsNumber;
    private @Nullable Component modAttributionComponent;

    public CommandHelpBuilder(@NotNull CommandDispatcher<CommandSourceStack> dispatcher, @NotNull CommandSourceStack source, @NotNull String commandName, @Nullable List<String> aliases, @Nullable List<String> pathSegments, @Nullable String displayPath) {
        this.dispatcher = dispatcher;
        this.commandNode = dispatcher.getRoot().getChild(commandName);
        this.redirectNode = commandNode != null ? commandNode.getRedirect() : null;
        this.source = source;
        this.commandName = commandName;
        this.aliases = aliases != null ? List.copyOf(aliases) : List.of();
        this.commandMetadata = ModMetadataRegistry.getCommandMetadata(getBaseCommandName());
        this.pathSegments = pathSegments != null ? List.copyOf(pathSegments) : List.of();
        this.displayPath = "/" + (displayPath != null ? displayPath : commandName);
        this.modMetadata = commandMetadata != null ? ModMetadataRegistry.getModMetadata(commandMetadata.getModId()) : null;
    }

    public @NotNull CommandHelpBuilder withDescription() {
        if (redirectNode != null) {
            descriptionComponent = Component.empty();
            descriptionComponent
                    .append(Component.literal(TranslationManager.translate(source, "betterhelp.commands.help.alias_of") + " ").setStyle(Constants.STYLE_BODY))
                    .append(new CommandHelpBuilder(dispatcher, source, getBaseCommandName(), aliases, null, null).withAliases(false).buildAsHover())
                    .append(Component.literal(". ").setStyle(Constants.STYLE_BODY));
        }

        String descriptionText = ModMetadataRegistry.getDescription(commandMetadata, source, getFullPath(getBaseCommandName()));
        if (descriptionText != null) {
            if (descriptionComponent == null) {
                descriptionComponent = Component.empty();
            }
            descriptionComponent.append(Component.literal(descriptionText).setStyle(Constants.STYLE_BODY));
        }

        return this;
    }

    public @NotNull CommandHelpBuilder withAliases(boolean asNumber) {
        if (aliases.isEmpty() || redirectNode != null) {
            return this;
        }

        MutableComponent component = Component.empty().append(Component.literal(TranslationManager.translate(source, "betterhelp.commands.help.aliases") + " ").setStyle(Constants.STYLE_ALIAS)).setStyle(Constants.STYLE_BODY);
        boolean isFirst = true;
        for (String alias : aliases) {
            if (!isFirst) {
                component.append(", ");
            } else {
                isFirst = false;
            }

            component.append(new CommandHelpBuilder(dispatcher, source, alias, null, null, null).buildAsHover());
        }

        aliasListComponent = component;
        aliasesAsNumber = asNumber;

        return this;
    }

    public @NotNull CommandHelpBuilder withUsage(int maxLines) {
        CommandNode<CommandSourceStack> node = redirectNode != null ? redirectNode : commandNode;
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
            String homepageLink = modMetadata.getHomepageUrl();
            modAttributionComponent = homepageLink != null
                    ? Components.link(source, modMetadata.getName(), URI.create(homepageLink), "%s.commands.help.homepage_hover".formatted(Constants.MOD_ID))
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
        MutableComponent aliasComponent = null;
        MutableComponent commandDetailComponent = Component.empty();
        if (asHover) {
            commandDetailComponent.append(Component.literal(displayPath).setStyle(Constants.STYLE_COMMAND_NAME));
        }
        if (descriptionComponent != null) {
            commandDetailComponent
                    .append("\n")
                    .append(descriptionComponent.setStyle(Constants.STYLE_BODY));
        }
        if (aliasListComponent != null) {
            if (aliasesAsNumber) {
                HoverEvent aliasesHoverEvent = new HoverEvent.ShowText(aliasListComponent);
                aliasComponent = Component.empty()
                        .append(" ")
                        .append(Component.literal("(+%s)".formatted(aliases.size())).setStyle(Constants.STYLE_ALIAS.withHoverEvent(aliasesHoverEvent)));
            } else {
                commandDetailComponent
                        .append("\n\n")
                        .append(aliasListComponent);
            }
        }
        if (usage != null && !usage.isEmpty() && usageMaxLines > 0) {
            commandDetailComponent.append("\n");
            if (!asHover) {
                commandDetailComponent
                        .append("\n")
                        .append(Component.literal(TranslationManager.translate(source, "betterhelp.commands.help.usage")).setStyle(Constants.STYLE_PRIMARY));
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

    private @NotNull List<String> getFullPath(@NotNull String commandName) {
        List<String> fullPath = new ArrayList<>();
        fullPath.add(commandName);
        fullPath.addAll(pathSegments);

        return List.copyOf(fullPath);
    }

    private @NotNull String getBaseCommandName() {
        return redirectNode != null ? redirectNode.getName() : commandName;
    }
}
