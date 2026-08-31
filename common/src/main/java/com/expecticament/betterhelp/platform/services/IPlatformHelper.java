package com.expecticament.betterhelp.platform.services;

import com.expecticament.betterhelp.metadata.ModMetadata;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public interface IPlatformHelper {

    /**
     * Gets the name of the current platform
     *
     * @return the name of the current platform.
     */
    @NotNull String getPlatformName();

    /**
     * Checks if a mod with the given id is loaded.
     *
     * @param modId the mod to check if it is loaded.
     * @return {code true} if the mod is loaded, {code false} otherwise.
     */
    boolean isModLoaded(String modId);

    /**
     * Check if the game is currently in a development environment.
     *
     * @return {code true} if in a development environment, {code false} otherwise.
     */
    boolean isDevelopmentEnvironment();

    /**
     * Scans other mods for metadata (descriptions, homepage links, etc.).
     * <p>
     * Only mods that actually contribute keys should be present in the map.
     *
     * @return an unmodifiable {@code modId} - {@code metadata} map.
     */
    @NotNull Map<String, ModMetadata> getModMetadata();
}
