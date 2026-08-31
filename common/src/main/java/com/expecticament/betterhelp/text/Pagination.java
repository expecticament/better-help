package com.expecticament.betterhelp.text;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.translation.TranslationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.*;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * A page of entries with navigation controls.
 */
public final class Pagination {
    public static final String PAGE_TAG_KEY = "page";

    /**
     * Builds a paginated message from all entries.
     *
     * @param source        used for hover translations.
     * @param entries       full list, not just the current page.
     * @param page          page index starting from 1. Out of range values are clamped.
     * @param pageSize      max entries per page.
     * @param entryRenderer turns one entry into a component.
     * @param pageClickId   {@link ClickEvent.Custom} id for page switches.
     * @return entries for the page, then controls if there is more than one page.
     */
    public static <T> @NotNull Component of(@NotNull CommandSourceStack source, @NotNull List<T> entries, int page, int pageSize, @NotNull Function<T, Component> entryRenderer, @NotNull Identifier pageClickId) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }

        int pageCount = getPageCount(entries.size(), pageSize);
        int currentPage = clampPage(page, pageCount);

        MutableComponent message = Component.empty();
        if (!entries.isEmpty()) {
            int from = (currentPage - 1) * pageSize;
            int to = Math.min(from + pageSize, entries.size());

            for (int i = from; i < to; i++) {
                if (i > from) {
                    message.append("\n");
                }

                message.append(entryRenderer.apply(entries.get(i)));
            }
        }

        if (pageCount > 1) {
            if (!entries.isEmpty()) {
                message.append("\n");
            }

            message.append(getControls(source, currentPage, pageCount, pageClickId));
        }

        return message;
    }

    public static int getPageCount(int totalEntries, int pageSize) {
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize must be >= 1");
        }
        if (totalEntries <= 0) {
            return 1;
        }

        return (totalEntries + pageSize - 1) / pageSize;
    }

    public static int clampPage(int page, int pageCount) {
        return Math.clamp(page, 1, pageCount);
    }

    public static @NotNull ClickEvent clickToPage(@NotNull Identifier pageClickId, int page) {
        CompoundTag tag = new CompoundTag();
        tag.putInt(PAGE_TAG_KEY, Math.max(1, page));

        return new ClickEvent.Custom(pageClickId, Optional.of(tag));
    }

    public static int readPage(@Nullable Tag tag) {
        if (tag instanceof CompoundTag compoundTag) {
            return Math.max(1, compoundTag.getIntOr(PAGE_TAG_KEY, 1));
        }

        return 1;
    }

    private static @NotNull MutableComponent getControls(@NotNull CommandSourceStack source, int currentPage, int pageCount, @NotNull Identifier pageClickId) {
        MutableComponent component = Component.literal("\n" + Constants.SYMBOL_PAGINATION_DECORATION)
                .append(Component.literal(" "))
                .append(arrow(source, true, currentPage > 1, currentPage - 1, pageClickId))
                .append(Component.literal(" "))
                .setStyle(Constants.STYLE_PAGINATION_DECORATION);

        for (int p = 1; p <= pageCount; p++) {
            if (p > 1) {
                component.append(Component.literal(" "));
            }

            component.append(pageMarker(source, p, p == currentPage, pageClickId));
        }

        component.append(Component.literal(" "))
                .append(arrow(source, false, currentPage < pageCount, currentPage + 1, pageClickId))
                .append(Component.literal(" "))
                .append(Component.literal(Constants.SYMBOL_PAGINATION_DECORATION).setStyle(Constants.STYLE_PAGINATION_DECORATION));

        return component;
    }

    private static @NotNull MutableComponent arrow(@NotNull CommandSourceStack source, boolean previous, boolean enabled, int targetPage, @NotNull Identifier pageClickId) {
        String hoverKey = previous ? "%s.pagination.previous".formatted(Constants.MOD_ID) : "%s.pagination.next".formatted(Constants.MOD_ID);
        ClickEvent clickEvent = enabled ? clickToPage(pageClickId, targetPage) : null;
        HoverEvent hoverEvent = enabled ? new HoverEvent.ShowText(Component.literal(TranslationManager.translate(source, hoverKey))) : null;

        return Components.button(
                "",
                previous ? Constants.SYMBOL_PAGINATION_PREV : Constants.SYMBOL_PAGINATION_NEXT,
                (enabled ? Constants.STYLE_PAGINATION_NAV_ACTIVE : Constants.STYLE_PAGINATION_NAV_INACTIVE)
                        .withClickEvent(clickEvent)
                        .withHoverEvent(hoverEvent)
        );
    }

    private static @NotNull MutableComponent pageMarker(@NotNull CommandSourceStack source, int page, boolean active, @NotNull Identifier pageClickId) {
        if (active) {
            return Component.literal(Integer.toString(page)).setStyle(Constants.STYLE_PAGINATION_MARKER_ACTIVE);
        }

        String hoverText = TranslationManager.translate(source, "%s.pagination.goto".formatted(Constants.MOD_ID)).formatted(page);
        HoverEvent hover = new HoverEvent.ShowText(Component.literal(hoverText));

        return Component.literal(Constants.SYMBOL_PAGINATION_MARKER).setStyle(Constants.STYLE_PAGINATION_MARKER_INACTIVE
                .withClickEvent(clickToPage(pageClickId, page))
                .withHoverEvent(hover)
        );
    }
}
