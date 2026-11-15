package com.expecticament.betterhelp.metadata;

import com.expecticament.betterhelp.Constants;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Set;

/**
 * Metadata provided by a mod through its language files.
 */
public final class ModMetadata {
    private final String id;
    private final String name;
    private final URI homepageUri;
    private final Set<ModCommandMetadata> commands;

    /**
     * @param id          the mod's id.
     * @param name        the mod's display name. Falls back to {@code id} if blank.
     * @param homepageUrl the mod's homepage URL. Ignored if blank or invalid.
     * @param commands    the commands this mod provides metadata for.
     */
    public ModMetadata(@NotNull String id, @NotNull String name, @Nullable String homepageUrl, @NotNull Set<ModCommandMetadata> commands) {
        this.id = id;
        this.name = name.isBlank() ? id : name;
        this.homepageUri = parseHomepageUri(id, homepageUrl);
        this.commands = Set.copyOf(commands);
    }

    /**
     * Parses a homepage URL. Only allows {@code http}/{@code https} URLs with a host.
     *
     * @param id          the mod's id.
     * @param homepageUrl the raw URL to parse.
     * @return the parsed URI, or {@code null} if the URL is missing or invalid.
     */
    private static @Nullable URI parseHomepageUri(@NotNull String id, @Nullable String homepageUrl) {
        if (homepageUrl == null || homepageUrl.isBlank()) {
            return null;
        }

        try {
            URI uri = new URI(homepageUrl.trim());
            String uriScheme = uri.getScheme();
            String scheme = uriScheme != null ? uriScheme.toLowerCase(Locale.ROOT) : null;
            if (("http".equals(scheme) || "https".equals(scheme)) && uri.getHost() != null) {
                return uri;
            }
        } catch (URISyntaxException ignored) {}

        Constants.LOGGER.warn("Invalid homepage URL for mod '{}': '{}'", id, homepageUrl);
        return null;
    }

    /**
     * @return the mod's id.
     */
    public @NotNull String getId() {
        return id;
    }

    /**
     * @return the mod's display name, or its id if no name was given.
     */
    public @NotNull String getName() {
        return name;
    }

    /**
     * @return the mod's homepage URI, or {@code null} if none was given, or it was invalid.
     */
    public @Nullable URI getHomepageUri() {
        return homepageUri;
    }

    /**
     * @return an unmodifiable set of the commands this mod provides metadata for.
     */
    public @NotNull Set<ModCommandMetadata> getCommands() {
        return commands;
    }
}
