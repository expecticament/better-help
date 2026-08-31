package com.expecticament.betterhelp.metadata;

import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata for a command provided by a mod through its language files.
 */
public final class ModCommandMetadata {
    private final String name;
    private final String modId;
    private final Map<String, Map<String, String>> descriptionTranslations;

    /**
     * @param name                     the command name.
     * @param modId                    the mod that owns this command metadata.
     * @param descriptionTranslations  a {@code language} - ({@code translation key} - {@code text}) map.
     */
    public ModCommandMetadata(@NotNull String name, @NotNull String modId, @NotNull Map<String, Map<String, String>> descriptionTranslations) {
        this.name = name;
        this.modId = modId;

        Map<String, Map<String, String>> unmodifiable = new HashMap<>();
        descriptionTranslations.forEach((language, keys) -> unmodifiable.put(language, Map.copyOf(keys)));
        this.descriptionTranslations = Map.copyOf(unmodifiable);
    }

    public @NotNull String getName() {
        return name;
    }

    public @NotNull String getModId() {
        return modId;
    }

    /**
     * Description keys by language.
     * <p>
     * Outer key is the language code (for example {@code en_us}).
     * Inner map is a {@code translation key} - {@code text} map.
     */
    public @NotNull Map<String, Map<String, String>> getDescriptionTranslations() {
        return descriptionTranslations;
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
}
