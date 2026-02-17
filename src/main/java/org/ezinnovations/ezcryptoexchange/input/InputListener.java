package org.ezinnovations.ezcryptoexchange.input;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;

public final class InputListener implements Listener {
    private final EZCryptoExchange plugin;

    public InputListener(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onChat(AsyncPlayerChatEvent event) {
        InputSession session = plugin.inputs().get(event.getPlayer().getUniqueId());
        if (session == null) {
            return;
        }
        event.setCancelled(true);
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase("cancel")) {
            plugin.inputs().clear(event.getPlayer().getUniqueId(), true);
            return;
        }
        session.timeoutTask().cancel();
        plugin.inputs().clear(event.getPlayer().getUniqueId(), false);
        session.callback().accept(msg);
    }
}
