package org.ezinnovations.ezcryptoexchange.util;

import org.bukkit.Sound;
import org.bukkit.entity.Player;

public final class SoundUtil {
    private SoundUtil() {
    }

    public static void play(Player player, String soundName) {
        if (soundName == null || soundName.isBlank()) {
            return;
        }
        try {
            player.playSound(player.getLocation(), Sound.valueOf(soundName), 0.9F, 1.0F);
        } catch (IllegalArgumentException ignored) {
            // fail silently by design
        }
    }
}
