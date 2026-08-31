package com.expecticament.betterhelp.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Metadata provided by a mod through its language files.
 */
public final class ModMetadata {
    private final String id;
    private final String name;
    private final String homepageUrl;
    private final Set<ModCommandMetadata> commands;

    public ModMetadata(@NotNull String id, @NotNull String name, @Nullable String homepageUrl, @NotNull Set<ModCommandMetadata> commands) {
        this.id = id;
        this.name = name.isBlank() ? id : name;
        this.homepageUrl = homepageUrl == null || homepageUrl.isBlank() ? null : homepageUrl;
        this.commands = Set.copyOf(commands);
    }

    public @NotNull String getId() {
        return id;
    }

    public @NotNull String getName() {
        return name;
    }

    public @Nullable String getHomepageUrl() {
        return homepageUrl;
    }

    public @NotNull Set<ModCommandMetadata> getCommands() {
        return commands;
    }
}
