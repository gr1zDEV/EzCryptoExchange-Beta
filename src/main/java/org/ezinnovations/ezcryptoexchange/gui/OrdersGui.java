package org.ezinnovations.ezcryptoexchange.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;
import org.ezinnovations.ezcryptoexchange.orders.Order;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public final class OrdersGui {
    private final EZCryptoExchange plugin;

    public OrdersGui(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, int page) {
        int current = Math.max(1, page);
        Inventory inventory = Bukkit.createInventory(new GuiHolder("orders", String.valueOf(current)), 54, plugin.msg("gui_orders_title"));
        for (int i = 0; i < 54; i++) inventory.setItem(i, GuiUtils.glass());
        try {
            List<Order> orders = plugin.orders().findOpen(player.getUniqueId(), 45, (current - 1) * 45);
            int slot = 0;
            for (Order order : orders) {
                inventory.setItem(slot++, GuiUtils.item(Material.PAPER, "&e" + order.side() + " " + order.asset() + " Limit",
                    List.of("&7Order ID: &f#" + order.id(), "&7Limit Price: &f$" + order.limitPrice(), "&7Created: &f" + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(order.createdAt())), "", "&cClick to cancel this order")));
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning(ex.getMessage());
        }
        inventory.setItem(47, GuiUtils.item(Material.ARROW, "&ePrevious", List.of()));
        inventory.setItem(49, GuiUtils.item(Material.BOOK, "&7Page " + current, List.of()));
        inventory.setItem(51, GuiUtils.item(Material.ARROW, "&eNext", List.of()));
        inventory.setItem(53, GuiUtils.item(Material.ARROW, "&eBack", List.of()));
        player.openInventory(inventory);
    }
}
