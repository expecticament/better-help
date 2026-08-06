package com.expecticament.betterhelp.platform;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.metadata.ModMetadata;
import com.expecticament.betterhelp.metadata.ModMetadataCollector;
import com.expecticament.betterhelp.platform.services.IPlatformHelper;
import net.neoforged.fml.ModList;
import net.neoforged.fml.jarcontents.JarContents;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforgespi.language.IModInfo;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.*;

public final class NeoForgePlatformHelper implements IPlatformHelper {

    @Override
    public String getPlatformName() {
        return "NeoForge";
    }

    @Override
    public boolean isModLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }

    @Override
    public boolean isDevelopmentEnvironment() {
        return !FMLLoader.getCurrent().isProduction();
    }

    @Override
    public Map<String, ModMetadata> getModMetadata() {
        Map<String, ModMetadataCollector> collectors = new LinkedHashMap<>();

        ModList.get().forEachModFile(modFile -> {
            Set<String> modIds = new HashSet<>();
            Map<String, String> modNamesById = new HashMap<>();

            for (IModInfo modInfo : modFile.getModInfos()) {
                String modId = modInfo.getModId();
                if (!modId.equals(Constants.MOD_ID)) {
                    modIds.add(modId);
                    modNamesById.put(modId, modInfo.getDisplayName());
                }
            }
            if (modIds.isEmpty()) {
                return;
            }

            JarContents contents = modFile.getContents();
            contents.visitContent("assets", (relativePath, resource) -> {
                if (!relativePath.endsWith(".json")) {
                    return;
                }

                // assets/(namespace)/lang/(lang).json
                String[] parts = relativePath.split("/");
                if (parts.length != 4 || !parts[2].equals("lang")) {
                    return;
                }

                String namespace = parts[1];
                if (!modIds.contains(namespace)) {
                    return;
                }

                String language = ModMetadataCollector.stripExtension(parts[3]);
                ModMetadataCollector collector = collectors.computeIfAbsent(namespace, id -> new ModMetadataCollector(id, modNamesById.getOrDefault(id, id)));

                try (Reader reader = resource.bufferedReader(StandardCharsets.UTF_8)) {
                    collector.mergeLanguageFile(language, reader);
                } catch (Exception e) {
                    Constants.LOGGER.error("Failed to read {} from {}", relativePath, modFile.getFileName(), e);
                }
            });
        });

        Map<String, ModMetadata> result = new LinkedHashMap<>();
        collectors.forEach((modId, collector) -> {
            ModMetadata metadata = collector.build();
            if (metadata != null) {
                result.put(modId, metadata);
            }
        });

        return Map.copyOf(result);
    }
}
