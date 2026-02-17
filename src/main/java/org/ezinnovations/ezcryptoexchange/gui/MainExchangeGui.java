package org.ezinnovations.ezcryptoexchange.gui;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;
import org.ezinnovations.ezcryptoexchange.prices.Asset;
import org.ezinnovations.ezcryptoexchange.util.BigDecimalUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public final class MainExchangeGui {
    private final EZCryptoExchange plugin;

    public MainExchangeGui(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    public void open(Player player) {
        Inventory inventory = Bukkit.createInventory(new GuiHolder("main", ""), 54, plugin.msg("gui_main_title"));
        fill(inventory);
        try {
            String wallet = plugin.wallets().ensureWallet(player.getUniqueId(), player.getName());
            inventory.setItem(3, GuiUtils.item(Material.PLAYER_HEAD, "&6Your Wallet", List.of("&7ID: " + wallet)));
        } catch (SQLException e) {
            plugin.getLogger().warning(e.getMessage());
        }
        BigDecimal usd = plugin.vault().getBalance(player);
        inventory.setItem(5, GuiUtils.item(Material.GOLD_NUGGET, "&6USD", List.of("&a$" + BigDecimalUtil.format(usd, plugin.getConfig().getString("currency_format", "#,##0.00")))));
        for (Asset asset : Asset.values()) {
            int tile = asset == Asset.BTC ? 21 : asset == Asset.ETH ? 23 : 25;
            int buy = asset == Asset.BTC ? 30 : asset == Asset.ETH ? 32 : 34;
            int sell = asset == Asset.BTC ? 39 : asset == Asset.ETH ? 41 : 43;
            Optional<BigDecimal> price = plugin.prices().getPrice(asset);
            inventory.setItem(tile, GuiUtils.item(asset.material(), "&b" + asset.displayName(), List.of(price.map(p -> "&7Price: &f$" + BigDecimalUtil.format(p, "#,##0.00")).orElse("&cPrice unavailable"))));
            ItemStack buyItem = plugin.vault().isAvailable()
                ? GuiUtils.item(Material.LIME_WOOL, "&aBuy " + asset.name(), List.of("&7Open buy/sell menu"))
                : GuiUtils.item(Material.RED_WOOL, "&cBuy " + asset.name(), List.of(plugin.msg("vault_missing")));
            ItemStack sellItem = plugin.vault().isAvailable()
                ? GuiUtils.item(Material.RED_WOOL, "&cSell " + asset.name(), List.of("&7Open buy/sell menu"))
                : GuiUtils.item(Material.RED_WOOL, "&cSell " + asset.name(), List.of(plugin.msg("vault_missing")));
            inventory.setItem(buy, buyItem);
            inventory.setItem(sell, sellItem);
        }
        inventory.setItem(46, GuiUtils.item(Material.BOOK, "&eOpen Orders", List.of()));
        inventory.setItem(52, GuiUtils.item(Material.COMPASS, "&7Refresh", List.of()));
        player.openInventory(inventory);
    }

    private void fill(Inventory inventory) {
        for (int i = 0; i < inventory.getSize(); i++) {
            inventory.setItem(i, GuiUtils.glass());
        }
    }
}
