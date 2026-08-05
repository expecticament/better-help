package com.expecticament.betterhelp.translation;

import com.expecticament.betterhelp.BetterHelp;
import com.expecticament.betterhelp.Constants;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TranslationManager {
    private static final Gson GSON = new Gson();
    private static final String FALLBACK_LANG = "en_us";

    private static final Map<String, Map<String, String>> TRANSLATIONS = new HashMap<>();

    public static void init() {
        TRANSLATIONS.clear();
        loadBuiltinLanguage(FALLBACK_LANG);
    }

    public static @NotNull String languageOf(@NotNull CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return player != null ? player.clientInformation().language() : FALLBACK_LANG;
    }

    public static @NotNull String getFallbackLang() {
        return FALLBACK_LANG;
    }

    public static @NotNull String translate(@NotNull String language, @NotNull String key) {
        String translated = lookup(language, key);
        if (translated != null) {
            return translated;
        }

        if (!language.equals(FALLBACK_LANG)) {
            String fallback = lookup(FALLBACK_LANG, key);
            if (fallback != null) {
                return fallback;
            }
        }

        return key;
    }

    public static @NotNull String translate(@NotNull ServerPlayer player, @NotNull String key) {
        return translate(player.clientInformation().language(), key);
    }

    public static @NotNull String translate(@NotNull CommandSourceStack source, @NotNull String key) {
        return translate(languageOf(source), key);
    }

    private static @Nullable String lookup(@NotNull String lang, @NotNull String key) {
        return loadBuiltinLanguage(lang).get(key);
    }

    private static @NotNull Map<String, String> loadBuiltinLanguage(@NotNull String lang) {
        Map<String, String> cached = TRANSLATIONS.get(lang);
        if (cached != null) {
            return cached;
        }

        Map<String, String> fromAssets = readAssetLanguage(lang);
        Map<String, String> immutable = fromAssets != null ? Collections.unmodifiableMap(fromAssets) : Map.of();
        TRANSLATIONS.put(lang, immutable);

        return immutable;
    }

    private static @Nullable Map<String, String> readAssetLanguage(@NotNull String lang) {
        String path = "/assets/%s/lang/%s.json".formatted(Constants.MOD_ID, lang);
        try (InputStream stream = BetterHelp.class.getResourceAsStream(path)) {
            if (stream == null) {
                return null;
            }

            try (Reader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                if (json == null) {
                    return null;
                }

                Map<String, String> translations = new HashMap<>();
                json.entrySet().forEach(entry -> {
                    if (entry.getValue().isJsonPrimitive()) {
                        translations.put(entry.getKey(), entry.getValue().getAsString());
                    }
                });

                return translations;
            }
        } catch (Exception e) {
            Constants.LOGGER.error("Failed to load language file {}: {}", path, e.getMessage());
            return null;
        }
    }
}
