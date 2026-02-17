package org.ezinnovations.ezcryptoexchange.orders;

import org.bukkit.Bukkit;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;

public final class LimitOrderExecutor {
    private final EZCryptoExchange plugin;
    private final OrderManager orderManager;
    private int taskId = -1;

    public LimitOrderExecutor(EZCryptoExchange plugin, OrderManager orderManager) {
        this.plugin = plugin;
        this.orderManager = orderManager;
    }

    public void start(int seconds) {
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, orderManager::executeLimitFills, 100L, seconds * 20L).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
}
