package org.ezinnovations.ezcryptoexchange.util;

import org.bukkit.ChatColor;

public final class ColorUtil {
    private ColorUtil() {
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
