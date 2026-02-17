package org.ezinnovations.ezcryptoexchange.gui;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;
import org.ezinnovations.ezcryptoexchange.input.InputSession;
import org.ezinnovations.ezcryptoexchange.orders.OrderSide;
import org.ezinnovations.ezcryptoexchange.prices.Asset;
import org.ezinnovations.ezcryptoexchange.util.BigDecimalUtil;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Optional;

public final class GuiListener implements Listener {
    private final EZCryptoExchange plugin;

    public GuiListener(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onOpen(InventoryOpenEvent event) {
        plugin.inputs().clear(event.getPlayer().getUniqueId(), false);
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof GuiHolder holder)) {
            return;
        }
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player)) {
            return;
        }
        if (event.getClick() != ClickType.LEFT) {
            return;
        }
        ItemStack current = event.getCurrentItem();
        if (current == null || current.getType() == Material.AIR) {
            return;
        }
        switch (holder.type()) {
            case "main" -> handleMain(player, event.getSlot(), current);
            case "asset" -> handleAsset(player, Asset.valueOf(holder.data()), event.getSlot(), current);
            case "orders" -> handleOrders(player, holder.data(), event.getSlot(), current);
            case "confirm" -> handleConfirm(player, Long.parseLong(holder.data()), event.getSlot());
            default -> {
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof GuiHolder) {
            event.setCancelled(true);
        }
    }

    private void handleMain(Player player, int slot, ItemStack item) {
        if (slot == 46) {
            new OrdersGui(plugin).open(player, 1);
            return;
        }
        if (slot == 52) {
            new MainExchangeGui(plugin).open(player);
            return;
        }
        if (slot == 30 || slot == 39) new AssetPageGui(plugin).open(player, Asset.BTC);
        if (slot == 32 || slot == 41) new AssetPageGui(plugin).open(player, Asset.ETH);
        if (slot == 34 || slot == 43) new AssetPageGui(plugin).open(player, Asset.SOL);
    }

    private void handleAsset(Player player, Asset asset, int slot, ItemStack item) {
        if (slot == 0) {
            new MainExchangeGui(plugin).open(player);
            return;
        }
        if (slot == 19 || slot == 20 || slot == 21 || slot == 28) {
            BigDecimal usd = new BigDecimal(item.getItemMeta().getDisplayName().replace("§a$", ""));
            plugin.orderManager().marketBuy(player, asset, usd);
        } else if (slot == 29) {
            plugin.orderManager().marketBuy(player, asset, plugin.vault().getBalance(player));
        } else if (slot == 30) {
            player.closeInventory();
            plugin.inputs().register(player, InputSession.InputType.CUSTOM_BUY, raw -> BigDecimalUtil.parse(raw).ifPresentOrElse(v -> plugin.orderManager().marketBuy(player, asset, v), () -> player.sendMessage(plugin.msg("input_invalid_number"))), plugin.msg("input_prompt_amount"));
        } else if (slot == 24 || slot == 25 || slot == 26) {
            try {
                BigDecimal holding = plugin.balances().get(player.getUniqueId(), asset);
                BigDecimal pct = slot == 24 ? new BigDecimal("0.25") : slot == 25 ? new BigDecimal("0.50") : BigDecimal.ONE;
                plugin.orderManager().marketSell(player, asset, holding.multiply(pct));
            } catch (SQLException ex) {
                plugin.getLogger().warning(ex.getMessage());
            }
        } else if (slot == 33) {
            player.closeInventory();
            plugin.inputs().register(player, InputSession.InputType.CUSTOM_SELL, raw -> BigDecimalUtil.parse(raw).ifPresentOrElse(v -> plugin.orderManager().marketSell(player, asset, v), () -> player.sendMessage(plugin.msg("input_invalid_number"))), plugin.msg("input_prompt_asset_amount", plugin.map("asset", asset.name())));
        } else if (slot == 46 || slot == 48) {
            OrderSide side = slot == 46 ? OrderSide.BUY : OrderSide.SELL;
            player.closeInventory();
            plugin.inputs().register(player, InputSession.InputType.LIMIT_PRICE, priceRaw -> {
                Optional<BigDecimal> p = BigDecimalUtil.parse(priceRaw);
                if (p.isEmpty()) {
                    player.sendMessage(plugin.msg("input_invalid_number"));
                    return;
                }
                plugin.inputs().register(player, InputSession.InputType.LIMIT_AMOUNT, amountRaw -> {
                    Optional<BigDecimal> a = BigDecimalUtil.parse(amountRaw);
                    if (a.isEmpty()) {
                        player.sendMessage(plugin.msg("input_invalid_number"));
                        return;
                    }
                    plugin.orderManager().placeLimit(player, side, asset, p.get(), a.get());
                }, side == OrderSide.BUY ? plugin.msg("input_prompt_amount") : plugin.msg("input_prompt_asset_amount", plugin.map("asset", asset.name())));
            }, plugin.msg("input_prompt_limit_price"));
        }
    }

    private void handleOrders(Player player, String pageData, int slot, ItemStack item) {
        int page = Integer.parseInt(pageData);
        if (slot == 53) {
            new MainExchangeGui(plugin).open(player);
        } else if (slot == 47) {
            new OrdersGui(plugin).open(player, Math.max(1, page - 1));
        } else if (slot == 51) {
            new OrdersGui(plugin).open(player, page + 1);
        } else if (slot < 45 && item.getType() == Material.PAPER && item.getItemMeta().getLore() != null) {
            String line = item.getItemMeta().getLore().get(0);
            long id = Long.parseLong(line.replace("§7Order ID: §f#", ""));
            try {
                plugin.orders().findById(id).ifPresent(order -> new ConfirmCancelGui(plugin).open(player, order));
            } catch (SQLException ex) {
                plugin.getLogger().warning(ex.getMessage());
            }
        }
    }

    private void handleConfirm(Player player, long id, int slot) {
        if (slot == 11) {
            plugin.orderManager().cancelOrder(player, id);
            new OrdersGui(plugin).open(player, 1);
        }
        if (slot == 15) {
            new OrdersGui(plugin).open(player, 1);
        }
    }
}
