package com.expecticament.betterhelp.text;

import com.expecticament.betterhelp.Constants;
import com.expecticament.betterhelp.translation.TranslationManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.*;
import org.jetbrains.annotations.NotNull;

import java.net.URI;

public class Components {
    public static @NotNull Component intoBulletedListEntry(@NotNull Component component) {
        String bulletPoint = Constants.SYMBOL_BULLET_POINT;
        Style style = Constants.STYLE_LIST_ENTRY;

        return Component.literal(bulletPoint).setStyle(style).append(component);
    }

    public static @NotNull MutableComponent button(@NotNull String label, @NotNull String icon, @NotNull Style style) {
        return Component.literal(Constants.SYMBOL_BUTTON_PREFIX)
                .append(!icon.isBlank() ? icon : "")
                .append(!icon.isBlank() && !label.isBlank() ? Constants.SYMBOL_BUTTON_SEPARATOR : "")
                .append(Component.literal(!label.isBlank() ? label : ""))
                .append(Component.literal(Constants.SYMBOL_BUTTON_SUFFIX))
                .setStyle(style);
    }

    public static @NotNull Component link(@NotNull CommandSourceStack source, @NotNull String text, @NotNull URI uri, @NotNull String hoverTranslationKey) {
        return Component.literal(text).setStyle(Constants.STYLE_LINK
                .withHoverEvent(new HoverEvent.ShowText(Component.literal(TranslationManager.translate(source, hoverTranslationKey)).setStyle(Constants.STYLE_BODY)))
                .withClickEvent(new ClickEvent.OpenUrl(uri))
        );
    }
}
