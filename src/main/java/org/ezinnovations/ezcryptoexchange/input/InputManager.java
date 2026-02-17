package org.ezinnovations.ezcryptoexchange.input;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public final class InputManager {
    private final EZCryptoExchange plugin;
    private final Map<UUID, InputSession> sessions = new ConcurrentHashMap<>();

    public InputManager(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    public void register(Player player, InputSession.InputType type, Consumer<String> callback, String prompt) {
        clear(player.getUniqueId(), false);
        BukkitTask timeout = plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            sessions.remove(player.getUniqueId());
            plugin.send(player, "input_timeout");
        }, plugin.getConfig().getInt("input_timeout_seconds", 30) * 20L);
        sessions.put(player.getUniqueId(), new InputSession(player.getUniqueId(), callback, type, timeout));
        player.sendMessage(prompt);
    }

    public InputSession get(UUID uuid) {
        return sessions.get(uuid);
    }

    public void clear(UUID uuid, boolean notify) {
        InputSession session = sessions.remove(uuid);
        if (session != null) {
            session.timeoutTask().cancel();
            if (notify) {
                plugin.getServer().getPlayer(uuid).sendMessage(plugin.msg("input_cancelled"));
            }
        }
    }

    public void clearAll() {
        sessions.values().forEach(session -> session.timeoutTask().cancel());
        sessions.clear();
    }
}
