package com.expecticament.betterhelp.metadata;

import com.expecticament.betterhelp.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata for a command provided by a mod through its language files.
 * <p>
 * Descriptions are stored by path without the {@code commands.(command).}/{@code (modId).commands.(command).} prefix
 * or the {@code .description} suffix. For the command {@code examplecommand}:
 * <ul>
 *   <li>{@code commands.examplecommand.description} - {@code ""} (the command itself)</li>
 *   <li>{@code commands.examplecommand.arg1.description} - {@code arg1}</li>
 *   <li>{@code commands.examplecommand.arg1.targets.description} - {@code arg1.targets}</li>
 * </ul>
 * <p>
 * Two instances are equal if they have the same command name.
 */
public final class ModCommandMetadata {
    private static final String COMMANDS_PREFIX = "commands.";
    private static final String DESCRIPTION_SUFFIX = ".description";

    private final String name;
    private final String modId;
    private final Map<String, Map<String, String>> descriptions;

    /**
     * @param name         the command's name, for example {@code examplecommand}. Must not be blank.
     * @param modId        the id of the mod that provides this metadata. Must not be blank.
     * @param descriptions a {@code language} - ({@code path} - {@code text}) map. Paths don't include the
     *                     command name, for example {@code arg1.targets}. {@code ""} is the command itself.
     * @throws IllegalArgumentException if the {@code name} or {@code modId} is blank.
     */
    public ModCommandMetadata(@NotNull String name, @NotNull String modId, @NotNull Map<String, Map<String, String>> descriptions) {
        boolean modIdBlank = modId.isBlank();
        if (name.isBlank()) {
            throw new IllegalArgumentException("Command name must not be blank" + (modIdBlank ? "" : " (mod '%s')".formatted(modId)));
        }
        if (modIdBlank) {
            throw new IllegalArgumentException("Mod id must not be blank (command '%s')".formatted(name));
        }

        this.name = name;
        this.modId = modId;

        Map<String, Map<String, String>> validDescriptions = new HashMap<>();
        descriptions.forEach((language, byPath) -> {
            Map<String, String> validByPath = new HashMap<>();
            byPath.forEach((path, text) -> {
                if (isValidPath(path)) {
                    validByPath.put(path, text);
                } else {
                    Constants.LOGGER.warn("Ignoring invalid command description path '{}' for command '{}' from mod '{}'", path, name, modId);
                }
            });

            if (!validByPath.isEmpty()) {
                validDescriptions.put(language, Map.copyOf(validByPath));
            }
        });
        this.descriptions = Map.copyOf(validDescriptions);
    }

    /**
     * Parses a description key from a language file.
     * <p>
     * Accepts {@code (modId).commands.(command).(path).description}. The {@code (modId).} prefix
     * and the {@code .(path)} part are optional.
     * For example, {@code commands.examplecommand.description} or
     * {@code examplemod.commands.examplecommand.arg1.description}.
     *
     * @param modId the id of the mod that owns the language file.
     * @param key   the translation key.
     * @return the {@link DescriptionKey}, or {@code null} if the key isn't a valid description key.
     */
    public static @Nullable DescriptionKey parseDescriptionKey(@NotNull String modId, @NotNull String key) {
        String modPrefix = modId + ".";
        if (key.startsWith(modPrefix)) {
            DescriptionKey unprefixed = parseUnprefixedDescriptionKey(key.substring(modPrefix.length()));
            if (unprefixed != null) {
                return unprefixed;
            }
        }

        return parseUnprefixedDescriptionKey(key);
    }

    /**
     * Parses a description key from a language file.
     * <p>
     * Accepts {@code commands.(command).(path).description}. For example,
     * {@code commands.examplecommand.description}.
     *
     * @param key a translation key without the mod id prefix.
     * @return the {@link DescriptionKey}, or {@code null} if the key isn't a valid description key.
     */
    private static @Nullable DescriptionKey parseUnprefixedDescriptionKey(@NotNull String key) {
        int keyLength = key.length();
        int cmdPrefixLength = COMMANDS_PREFIX.length();
        int descSuffixLength = DESCRIPTION_SUFFIX.length();
        if (!key.startsWith(COMMANDS_PREFIX) || !key.endsWith(DESCRIPTION_SUFFIX) || keyLength <= cmdPrefixLength + descSuffixLength) {
            return null;
        }

        String fullPath = key.substring(cmdPrefixLength, keyLength - descSuffixLength);
        int dot = fullPath.indexOf('.');
        String commandName = dot < 0 ? fullPath : fullPath.substring(0, dot);
        String path = dot < 0 ? "" : fullPath.substring(dot + 1);

        return !commandName.isBlank() && isValidPath(path) ? new DescriptionKey(commandName, path) : null;
    }

    /**
     * @param path a path without the command name, or {@code ""}.
     * @return {@code true} if the {@code path} is empty or dot-separated non-blank segments.
     */
    private static boolean isValidPath(@NotNull String path) {
        if (path.isEmpty()) {
            return true;
        }

        for (String segment : path.split("\\.", -1)) {
            if (segment.isBlank()) {
                return false;
            }
        }
        return true;
    }

    /**
     * @return the command's name.
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * @return the id of the mod that provides this metadata.
     */
    public @NotNull String getModId() {
        return modId;
    }

    /**
     * @return an unmodifiable {@code language} - ({@code path} - {@code text}) map.
     */
    public @NotNull Map<String, Map<String, String>> getDescriptions() {
        return descriptions;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof ModCommandMetadata other)) {
            return false;
        }

        return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    /**
     * A parsed description key.
     */
    public static final class DescriptionKey {
        private final String commandName;
        private final String path;

        /**
         * @param commandName the command's name, for example {@code examplecommand}.
         * @param path        the path without the command name, for example {@code arg1.targets},
         *                    or {@code ""} for the command itself.
         */
        private DescriptionKey(@NotNull String commandName, @NotNull String path) {
            this.commandName = commandName;
            this.path = path;
        }

        /**
         * @return the command's name.
         */
        public @NotNull String getCommandName() {
            return commandName;
        }

        /**
         * @return the path without the command name.
         */
        public @NotNull String getPath() {
            return path;
        }
    }
}
