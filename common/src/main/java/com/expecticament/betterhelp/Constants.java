package com.expecticament.betterhelp;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Constants {
    public static final String MOD_ID = "betterhelp";
    public static final String MOD_NAME = "Better /help";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);


    public static final Style STYLE_PRIMARY = Style.EMPTY.withColor(ChatFormatting.GOLD);
    public static final Style STYLE_SECONDARY = Style.EMPTY.withColor(ChatFormatting.AQUA);

    public static final Style STYLE_BODY = Style.EMPTY.withColor(ChatFormatting.WHITE);
    public static final Style STYLE_FADED = Style.EMPTY.withColor(ChatFormatting.GRAY);
    public static final Style STYLE_MUTED = Style.EMPTY.withColor(ChatFormatting.DARK_GRAY);

    public static final Style STYLE_LINK = STYLE_SECONDARY.withUnderlined(true);

    public static final Style STYLE_COMMAND_NAME = Style.EMPTY.withColor(ChatFormatting.GREEN);

    public static final Style STYLE_ALIAS = Style.EMPTY.withColor(TextColor.fromRgb(0x8CB3FF));

    public static final String SYMBOL_INFO = "ℹ";

    public static final String SYMBOL_PAGINATION_DECORATION = "▬▬▬▬▬▬";
    public static final Style STYLE_PAGINATION_DECORATION = STYLE_MUTED;

    public static final String SYMBOL_PAGINATION_PREV = "◀", SYMBOL_PAGINATION_NEXT = "▶";
    public static final Style STYLE_PAGINATION_NAV_ACTIVE = STYLE_SECONDARY, STYLE_PAGINATION_NAV_INACTIVE = STYLE_FADED;

    public static final String SYMBOL_PAGINATION_MARKER = "◆";
    public static final Style STYLE_PAGINATION_MARKER_ACTIVE = STYLE_PRIMARY.withBold(true), STYLE_PAGINATION_MARKER_INACTIVE = STYLE_FADED;

    public static final String SYMBOL_BUTTON_PREFIX = "[", SYMBOL_BUTTON_SUFFIX = "]", SYMBOL_BUTTON_SEPARATOR = " ";

    public static final String SYMBOL_BULLET_POINT = "• ";
    public static final Style STYLE_LIST_ENTRY = Style.EMPTY.withColor(ChatFormatting.YELLOW);
}