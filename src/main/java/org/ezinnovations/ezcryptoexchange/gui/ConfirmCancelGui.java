package org.ezinnovations.ezcryptoexchange.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;
import org.ezinnovations.ezcryptoexchange.orders.Order;

import java.util.List;

public final class ConfirmCancelGui {
    private final EZCryptoExchange plugin;

    public ConfirmCancelGui(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Order order) {
        Inventory inventory = Bukkit.createInventory(new GuiHolder("confirm", String.valueOf(order.id())), 27, plugin.msg("gui_confirm_cancel_title"));
        for (int i = 0; i < 27; i++) inventory.setItem(i, GuiUtils.glass());
        inventory.setItem(11, GuiUtils.item(Material.LIME_WOOL, "&aConfirm Cancel", List.of()));
        inventory.setItem(13, GuiUtils.item(Material.PAPER, "&eOrder #" + order.id(), List.of("&7" + order.side() + " " + order.asset())));
        inventory.setItem(15, GuiUtils.item(Material.RED_WOOL, "&cGo Back", List.of()));
        player.openInventory(inventory);
    }
}
