package com.expecticament.betterhelp.platform;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.metadata.ModMetadata;
import com.expecticament.betterhelp.metadata.ModMetadataCollector;
import com.expecticament.betterhelp.platform.services.IPlatformHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

public final class FabricPlatformHelper implements IPlatformHelper {

    @Override
    public @NotNull String getPlatformName() {
        return "Fabric";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return FabricLoader.getInstance().isModLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }

    @Override
    public @NotNull Map<String, ModMetadata> getModMetadata() {
        Map<String, ModMetadata> result = new LinkedHashMap<>();

        for (ModContainer mod : FabricLoader.getInstance().getAllMods()) {
            String modId = mod.getMetadata().getId();
            if (modId.equals(Constants.MOD_ID)) {
                continue;
            }

            ModMetadataCollector collector = new ModMetadataCollector(modId, mod.getMetadata().getName());

            for (Path root : mod.getRootPaths()) {
                Path langDir = root.resolve("assets").resolve(modId).resolve("lang");
                if (!Files.isDirectory(langDir)) {
                    continue;
                }

                try (Stream<Path> langFiles = Files.list(langDir)) {
                    langFiles
                            .filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                            .forEach(langFile -> {
                                String language = ModMetadataCollector.stripExtension(langFile.getFileName().toString());

                                try (Reader reader = Files.newBufferedReader(langFile, StandardCharsets.UTF_8)) {
                                    collector.mergeLanguageFile(language, reader);
                                } catch (Exception e) {
                                    Constants.LOGGER.error("Failed to read {} from mod {}", langFile, modId, e);
                                }
                            });
                } catch (IOException e) {
                    Constants.LOGGER.error("Failed to list lang files for mod {}", modId, e);
                }
            }

            ModMetadata metadata = collector.build();
            if (metadata != null) {
                result.put(modId, metadata);
            }
        }

        return Map.copyOf(result);
    }
}
