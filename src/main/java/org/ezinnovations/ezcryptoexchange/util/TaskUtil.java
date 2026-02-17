package org.ezinnovations.ezcryptoexchange.util;

import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public final class TaskUtil {
    private TaskUtil() {
    }

    public static void runSync(Plugin plugin, Runnable runnable) {
        if (Bukkit.isPrimaryThread()) {
            runnable.run();
            return;
        }
        Bukkit.getScheduler().runTask(plugin, runnable);
    }

    public static void runAsync(Plugin plugin, Runnable runnable) {
        Bukkit.getScheduler().runTaskAsynchronously(plugin, runnable);
    }
}
