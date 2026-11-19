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
 *   <li>{@code commands.(command).(path).description}/{@code (modId).commands.(command).(path).description} -
 *       segments are Brigadier node names ({@code Commands.literal}/{@code Commands.argument} names),
 *       and {@code .(path)} is optional (see {@link ModCommandMetadata#parseDescriptionKey})</li>
 *   <li>{@code betterhelp.homepage}/{@code betterhelp.homepage.(modId)} - optional homepage URL</li>
 * </ul>
 */
public final class ModMetadataCollector {
    private static final Gson GSON = new Gson();

    private static final String HOMEPAGE_KEY = Constants.MOD_ID + ".homepage";

    private final String id;
    private final String name;
    private String homepageUrl;

    /**
     * Descriptions grouped by command, then language:
     * {@code commandName} - ({@code language} - ({@code path} - {@code text})).
     * <p>
     * Paths are stored without the {@code commands.(command).}/{@code (modId).commands.(command).} prefix
     * or the {@code .description} suffix (see {@link ModCommandMetadata}).
     */
    private final Map<String, Map<String, Map<String, String>>> commandDescriptions = new HashMap<>();

    /**
     * Creates a collector for a mod.
     *
     * @param modId   the mod id (must match namespace ({@code assets/<modId>/lang/...})).
     * @param modName the mod name.
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
     * @param language the language code of this file, for example {@code en_us}.
     * @param reader   the open language JSON contents.
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

            ModCommandMetadata.DescriptionKey descriptionKey = ModCommandMetadata.parseDescriptionKey(id, key);
            if (descriptionKey == null) {
                continue;
            }

            commandDescriptions
                    .computeIfAbsent(descriptionKey.getCommandName(), ignored -> new HashMap<>())
                    .computeIfAbsent(language, ignored -> new HashMap<>())
                    .putIfAbsent(descriptionKey.getPath(), value);
        }
    }

    /**
     * Finishes collection and creates {@link ModMetadata}.
     * <p>
     * Returns {@code null} when this mod has no command descriptions.
     *
     * @return metadata for this mod, or {@code null} if nothing is found.
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
     * Removes a file extension from a file name.
     * <p>
     * Example: {@code en_us.json} becomes {@code en_us}.
     *
     * @param fileName the file name, with or without an extension.
     * @return the file name without an extension.
     */
    public static @NotNull String stripExtension(@NotNull String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return dotIndex >= 0 ? fileName.substring(0, dotIndex) : fileName;
    }
}
