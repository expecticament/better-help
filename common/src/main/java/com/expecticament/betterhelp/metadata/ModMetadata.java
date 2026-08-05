package com.expecticament.betterhelp.metadata;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Metadata provided by a mod through its language files.
 */
public final class ModMetadata {
    private final @NotNull String modId;
    private final @Nullable String homepageUrl;
    private final @NotNull Set<ModCommandMetadata> commands;

    public ModMetadata(@NotNull String modId, @Nullable String homepageUrl, @NotNull Set<ModCommandMetadata> commands) {
        this.modId = modId;
        this.homepageUrl = homepageUrl;
        this.commands = Set.copyOf(commands);
    }

    public @NotNull String getModId() {
        return modId;
    }

    public @Nullable String getHomepageUrl() {
        return homepageUrl;
    }

    public @NotNull Set<ModCommandMetadata> getCommands() {
        return commands;
    }
}
