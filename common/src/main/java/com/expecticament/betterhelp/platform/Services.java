package com.expecticament.betterhelp.platform;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.platform.services.IPlatformHelper;
import org.jetbrains.annotations.NotNull;

import java.util.ServiceLoader;

public final class Services {
    public static final IPlatformHelper PLATFORM = load(IPlatformHelper.class);

    public static <T> T load(@NotNull Class<T> clazz) {
        final T loadedService = ServiceLoader.load(clazz, Services.class.getClassLoader())
                .findFirst()
                .orElseThrow(() -> new NullPointerException("Failed to load service for " + clazz.getName()));
        Constants.LOGGER.debug("Loaded {} for service {}", loadedService, clazz);

        return loadedService;
    }
}