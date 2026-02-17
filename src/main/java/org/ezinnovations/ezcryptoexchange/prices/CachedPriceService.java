package org.ezinnovations.ezcryptoexchange.prices;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class CachedPriceService {
    private final JavaPlugin plugin;
    private final PriceProvider provider;
    private final Map<Asset, BigDecimal> cache = new ConcurrentHashMap<>();
    private volatile boolean stale;
    private int failures;
    private int taskId = -1;

    public CachedPriceService(JavaPlugin plugin, PriceProvider provider) {
        this.plugin = plugin;
        this.provider = provider;
    }

    public void start(int intervalSeconds) {
        taskId = Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> provider.fetchPrices()
            .thenAccept(prices -> {
                cache.putAll(prices);
                stale = false;
                failures = 0;
            })
            .exceptionally(ex -> {
                stale = true;
                failures++;
                if (failures % 5 == 0) {
                    plugin.getLogger().warning("Price fetch failed: " + ex.getMessage());
                }
                return null;
            }), 0L, intervalSeconds * 20L).getTaskId();
    }

    public void stop() {
        if (taskId != -1) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }

    public Optional<BigDecimal> getPrice(Asset asset) {
        return Optional.ofNullable(cache.get(asset));
    }

    public boolean isStale() {
        return stale;
    }
}
