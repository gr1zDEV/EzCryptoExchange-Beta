package org.ezinnovations.ezcryptoexchange.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;
import org.ezinnovations.ezcryptoexchange.prices.Asset;

import java.util.List;

public final class AssetPageGui {
    private final EZCryptoExchange plugin;

    public AssetPageGui(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    public void open(Player player, Asset asset) {
        Inventory inventory = Bukkit.createInventory(new GuiHolder("asset", asset.name()), 54, plugin.msg("gui_asset_title", plugin.map("asset", asset.name())));
        for (int i = 0; i < 54; i++) {
            inventory.setItem(i, GuiUtils.glass());
        }
        inventory.setItem(0, GuiUtils.item(Material.ARROW, "&eBack", List.of()));
        inventory.setItem(2, GuiUtils.item(asset.material(), "&b" + asset.displayName(), List.of()));
        inventory.setItem(19, GuiUtils.item(Material.LIME_STAINED_GLASS_PANE, "&a$10", List.of()));
        inventory.setItem(20, GuiUtils.item(Material.LIME_STAINED_GLASS_PANE, "&a$50", List.of()));
        inventory.setItem(21, GuiUtils.item(Material.LIME_STAINED_GLASS_PANE, "&a$100", List.of()));
        inventory.setItem(28, GuiUtils.item(Material.LIME_STAINED_GLASS_PANE, "&a$500", List.of()));
        inventory.setItem(29, GuiUtils.item(Material.LIME_WOOL, "&aMAX", List.of()));
        inventory.setItem(30, GuiUtils.item(Material.ANVIL, "&aCustom Amount", List.of()));
        inventory.setItem(24, GuiUtils.item(Material.RED_STAINED_GLASS_PANE, "&c25%", List.of()));
        inventory.setItem(25, GuiUtils.item(Material.RED_STAINED_GLASS_PANE, "&c50%", List.of()));
        inventory.setItem(26, GuiUtils.item(Material.RED_STAINED_GLASS_PANE, "&c100%", List.of()));
        inventory.setItem(33, GuiUtils.item(Material.ANVIL, "&cCustom Amount", List.of()));
        inventory.setItem(46, GuiUtils.item(Material.GOLD_INGOT, "&6Limit Buy", List.of()));
        inventory.setItem(48, GuiUtils.item(Material.GOLD_INGOT, "&6Limit Sell", List.of()));
        player.openInventory(inventory);
    }
}
