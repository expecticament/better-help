package com.expecticament.betterhelp.metadata;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.platform.Services;
import com.expecticament.betterhelp.translation.TranslationManager;
import net.minecraft.commands.CommandSourceStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds and looks up {@link ModMetadata} / {@link ModCommandMetadata} scanned from other mods.
 * <p>
 * Description lookup tries the owning command's translations first, then Better /help built-ins.
 */
public final class ModMetadataRegistry {

    /**
     * Metadata by mod id.
     * <p>
     * Insertion order matches scan order from the platform helper.
     */
    private static Map<String, ModMetadata> BY_MOD_ID = Map.of();

    /**
     * Command metadata by command name.
     * <p>
     * When several mods document the same command, the first scanned mod wins.
     */
    private static Map<String, ModCommandMetadata> BY_COMMAND = Map.of();

    /**
     * Clears the cache and loads metadata from every other installed mod.
     */
    public static void load() {
        Map<String, ModMetadata> loaded = Services.PLATFORM.getModMetadata();
        Map<String, ModMetadata> byModId = new LinkedHashMap<>(loaded);
        Map<String, ModCommandMetadata> byCommand = new LinkedHashMap<>();

        for (ModMetadata metadata : loaded.values()) {
            for (ModCommandMetadata command : metadata.getCommands()) {
                byCommand.putIfAbsent(command.getName(), command);
            }
        }

        BY_MOD_ID = Collections.unmodifiableMap(byModId);
        BY_COMMAND = Collections.unmodifiableMap(byCommand);
        Constants.LOGGER.info("Loaded metadata from {} mods ({} commands)", BY_MOD_ID.size(), BY_COMMAND.size());
    }

    /**
     * Returns metadata for a mod id, if that mod contributed any.
     *
     * @param modId The mod to look up.
     * @return That mod's metadata, or {@code null} if nothing is found.
     */
    public static @Nullable ModMetadata getModMetadata(@NotNull String modId) {
        return BY_MOD_ID.get(modId);
    }

    /**
     * Returns command metadata for this command name, if any mod documented it.
     * <p>
     * When several mods document the same command, the first scanned mod wins.
     *
     * @param commandName The command name.
     * @return The command metadata, or {@code null} if no mod documented this command.
     */
    public static @Nullable ModCommandMetadata getCommandMetadata(@NotNull String commandName) {
        return BY_COMMAND.get(commandName);
    }

    /**
     * Looks up a description for a command path (most specific path first).
     * <p>
     * If {@code command} is present, its translations are tried first (player language,
     * then the fallback language). If nothing matches, Better /help built-ins are used.
     *
     * @param command      The command metadata, or {@code null} to skip mod texts.
     * @param source       The command source. Used to determine the player's language.
     * @param pathSegments The command path pieces from Brigadier node names, for example
     *                     {@code ["advancement", "grant", "targets", "everything"]}.
     * @return The description text, or {@code null} if nothing is found.
     */
    public static @Nullable String getDescription(@Nullable ModCommandMetadata command, @NotNull CommandSourceStack source, @NotNull List<String> pathSegments) {
        if (pathSegments.isEmpty()) {
            return null;
        }

        if (command != null) {
            String fromMod = findModDescription(command, source, pathSegments);
            if (fromMod != null) {
                return fromMod;
            }
        }

        return getBuiltinDescription(source, pathSegments);
    }

    /**
     * Checks whether the command metadata has description for this path.
     * <p>
     * Built-in Better /help descriptions do not count.
     *
     * @param command      The command metadata to check.
     * @param source       The command source. Used to determine the player's language.
     * @param pathSegments The command path pieces from Brigadier node names, for example
     *                     {@code ["advancement", "grant", "targets", "everything"]}.
     * @return {@code true} if this command provides a description for the path, {@code false} otherwise.
     */
    public static boolean hasModDescription(@NotNull ModCommandMetadata command, @NotNull CommandSourceStack source, @NotNull List<String> pathSegments) {
        return findModDescription(command, source, pathSegments) != null;
    }

    /**
     * Finds a description in the command's translations (player language,
     * then the fallback language).
     *
     * @param command      The command to search.
     * @param source       The command source. Used to determine the player's language.
     * @param pathSegments The command path pieces from Brigadier node names, for example
     *                     {@code ["advancement", "grant", "targets", "everything"]}.
     * @return The description text, or {@code null} if this command has no matching key.
     */
    private static @Nullable String findModDescription(@NotNull ModCommandMetadata command, @NotNull CommandSourceStack source, @NotNull List<String> pathSegments) {
        String language = TranslationManager.languageOf(source);
        String fallbackLanguage = TranslationManager.getFallbackLang();

        String fromLanguage = findDescription(command, language, pathSegments);
        if (fromLanguage != null) {
            return fromLanguage;
        }

        if (!language.equals(fallbackLanguage)) {
            return findDescription(command, fallbackLanguage, pathSegments);
        }

        return null;
    }

    /**
     * Finds a description key in the command metadata for a language, trying longer paths first.
     *
     * @param command      The command to search.
     * @param language     The language code, for example {@code en_us}.
     * @param pathSegments The command path pieces from Brigadier node names, for example
     *                     {@code ["advancement", "grant", "targets", "everything"]}.
     * @return The description text, or {@code null} if this command has no matching key.
     */
    private static @Nullable String findDescription(@NotNull ModCommandMetadata command, @NotNull String language, @NotNull List<String> pathSegments) {
        Map<String, String> forLanguage = command.getDescriptionTranslations().get(language);
        if (forLanguage == null) {
            return null;
        }

        for (int length = pathSegments.size(); length >= 1; length--) {
            String key = "commands." + String.join(".", pathSegments.subList(0, length)) + ".description";
            String text = forLanguage.get(key);
            if (text != null && !text.isBlank()) {
                return text;
            }
        }

        return null;
    }

    /**
     * Looks up a built-in Better /help description (most specific path first).
     *
     * @param source       The command source. Used to determine the player's language.
     * @param pathSegments The command path pieces from Brigadier node names, for example
     *                     {@code ["advancement", "grant", "targets", "everything"]}.
     * @return The built-in description, or {@code null} if nothing is found.
     */
    private static @Nullable String getBuiltinDescription(@NotNull CommandSourceStack source, @NotNull List<String> pathSegments) {
        String language = TranslationManager.languageOf(source);
        for (int length = pathSegments.size(); length >= 1; length--) {
            String key = "commands." + String.join(".", pathSegments.subList(0, length)) + ".description";
            String builtin = TranslationManager.translate(language, key);
            if (!builtin.isBlank() && !builtin.equals(key)) {
                return builtin;
            }
        }

        return null;
    }
}
