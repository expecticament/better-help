package com.expecticament.betterhelp.metadata;

import com.expecticament.betterhelp.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Reader;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Builds {@link ModMetadata} for a mod by reading its language files.
 * <p>
 * Accepted keys:
 * <ul>
 *   <li>{@code commands.(path).description}/{@code (modid).commands.(path).description} -
 *       path segments are Brigadier node names ({@code Commands.literal}/{@code Commands.argument} names)</li>
 *   <li>{@code betterhelp.homepage}/{@code betterhelp.homepage.(modid)} - optional homepage URL</li>
 * </ul>
 */
public final class ModMetadataCollector {
    private static final Gson GSON = new Gson();

    private static final String HOMEPAGE_KEY = Constants.MOD_ID + ".homepage";
    private static final String COMMANDS_PREFIX = "commands.";
    private static final String DESCRIPTION_SUFFIX = ".description";

    private final @NotNull String id;
    private final @NotNull String name;
    private @Nullable String homepageUrl;

    /**
     * Description keys grouped by command, then language.
     * <p>
     * {@code commandName} - ({@code language} - ({@code description key} - {@code text})).
     */
    private final @NotNull Map<String, Map<String, Map<String, String>>> commandDescriptions = new HashMap<>();

    /**
     * Creates a collector for a mod.
     *
     * @param modId   The mod id (must match namespace ({@code assets/<modId>/lang/...})).
     * @param modName The mod name.
     */
    public ModMetadataCollector(@NotNull String modId, @NotNull String modName) {
        this.id = modId;
        this.name = modName;
    }

    /**
     * Reads a language JSON file and merges accepted keys into this collector.
     * <p>
     * Blank values and unknown keys are skipped.
     * The already existing values always win.
     *
     * @param language The language code of this file, for example {@code en_us}.
     * @param reader   The open language JSON contents.
     */
    public void mergeLanguageFile(@NotNull String language, @NotNull Reader reader) {
        JsonObject json = GSON.fromJson(reader, JsonObject.class);
        if (json == null) {
            return;
        }

        for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
            if (!entry.getValue().isJsonPrimitive()) {
                continue;
            }

            String key = entry.getKey();
            String value = entry.getValue().getAsString();
            if (value.isBlank()) {
                continue;
            }

            if (key.equals(HOMEPAGE_KEY + "." + id) || key.equals(HOMEPAGE_KEY)) {
                if (homepageUrl == null) {
                    homepageUrl = value;
                }
                continue;
            }

            String prefix = id + ".";
            if (key.startsWith(prefix)) {
                key = key.substring(prefix.length());
            }

            if (!isCommandDescriptionKey(key)) {
                continue;
            }

            String commandName = commandNameFromDescriptionKey(key);
            if (commandName == null) {
                continue;
            }

            commandDescriptions
                    .computeIfAbsent(commandName, ignored -> new HashMap<>())
                    .computeIfAbsent(language, ignored -> new HashMap<>())
                    .putIfAbsent(key, value);
        }
    }

    /**
     * Finishes collection and creates {@link ModMetadata}.
     * <p>
     * Returns {@code null} when this mod has no command descriptions.
     *
     * @return Metadata for this mod, or {@code null} if nothing is found.
     */
    public @Nullable ModMetadata build() {
        if (commandDescriptions.isEmpty()) {
            return null;
        }

        Set<ModCommandMetadata> commands = new HashSet<>();
        commandDescriptions.forEach((commandName, translations) -> {
            Map<String, Map<String, String>> cleaned = new HashMap<>();
            translations.forEach((language, keys) -> {
                if (!keys.isEmpty()) {
                    cleaned.put(language, keys);
                }
            });
            if (!cleaned.isEmpty()) {
                commands.add(new ModCommandMetadata(commandName, id, cleaned));
            }
        });

        if (commands.isEmpty()) {
            return null;
        }

        return new ModMetadata(id, name, homepageUrl, commands);
    }

    /**
     * Checks whether a lang key is a command description key.
     *
     * @param key The translation key from a language file.
     * @return {@code true} if the key looks like {@code commands.(path).description}, {@code false} otherwise.
     */
    private static boolean isCommandDescriptionKey(@NotNull String key) {
        return key.startsWith(COMMANDS_PREFIX) && key.endsWith(DESCRIPTION_SUFFIX) && key.length() > COMMANDS_PREFIX.length() + DESCRIPTION_SUFFIX.length();
    }

    /**
     * Extracts the command name from a description key.
     * <p>
     * Examples:
     * <ul>
     *   <li>{@code commands.example.description} - {@code example}</li>
     *   <li>{@code commands.example.targets.description} - {@code example}</li>
     * </ul>
     *
     * @param key A command description key.
     * @return The command name, or {@code null} if the path is empty.
     */
    private static @Nullable String commandNameFromDescriptionKey(@NotNull String key) {
        String path = key.substring(COMMANDS_PREFIX.length(), key.length() - DESCRIPTION_SUFFIX.length());
        if (path.isBlank()) {
            return null;
        }

        int dot = path.indexOf('.');
        return dot < 0 ? path : path.substring(0, dot);
    }

    /**
     * Removes a file extension from a file name.
     * <p>
     * Example: {@code en_us.json} becomes {@code en_us}.
     *
     * @param fileName The file name, with or without an extension.
     * @return The file name without an extension.
     */
    public static @NotNull String stripExtension(@NotNull String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
