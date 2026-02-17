package org.ezinnovations.ezcryptoexchange.orders;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;
import org.ezinnovations.ezcryptoexchange.economy.TransactionResult;
import org.ezinnovations.ezcryptoexchange.gui.AssetPageGui;
import org.ezinnovations.ezcryptoexchange.prices.Asset;
import org.ezinnovations.ezcryptoexchange.util.BigDecimalUtil;
import org.ezinnovations.ezcryptoexchange.util.Cooldowns;
import org.ezinnovations.ezcryptoexchange.util.TaskUtil;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class OrderManager implements Listener {
    private final EZCryptoExchange plugin;
    private final Map<UUID, Object> locks = new ConcurrentHashMap<>();
    private final Cooldowns cooldowns = new Cooldowns();
    private final Map<UUID, String> offlineQueue = new ConcurrentHashMap<>();

    public OrderManager(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    public void marketBuy(Player player, Asset asset, BigDecimal usdAmount) {
        if (!preTrade(player, usdAmount, true, asset)) return;
        executeLocked(player, () -> {
            BigDecimal market = plugin.prices().getPrice(asset).orElse(null);
            if (market == null) {
                plugin.send(player, "price_unavailable", plugin.map("asset", asset.name()));
                return;
            }
            BigDecimal spread = percent(plugin.getConfig().getDouble("trading.spread_percent", 0.1));
            BigDecimal ask = market.multiply(BigDecimal.ONE.add(spread));
            BigDecimal fee = usdAmount.multiply(percent(plugin.getConfig().getDouble("trading.fee_percent", 0.25))).setScale(2, RoundingMode.HALF_UP);
            BigDecimal net = usdAmount.subtract(fee);
            BigDecimal amount = net.divide(ask, 8, RoundingMode.HALF_UP);
            TransactionResult result = plugin.vault().withdraw(player, usdAmount);
            if (!result.success()) {
                plugin.send(player, "insufficient_usd", plugin.map("required", usdAmount.toPlainString(), "balance", plugin.vault().getBalance(player).toPlainString()));
                return;
            }
            try {
                BigDecimal current = plugin.balances().get(player.getUniqueId(), asset);
                plugin.balances().set(player.getUniqueId(), asset, current.add(amount));
                plugin.trades().insert(player.getUniqueId(), asset, OrderSide.BUY, ask, amount, usdAmount, fee);
                cooldowns.apply(player.getUniqueId());
                plugin.send(player, "buy_success", plugin.map("amount", amount.toPlainString(), "asset", asset.name(), "cost", usdAmount.toPlainString(), "fee", fee.toPlainString()));
            } catch (SQLException ex) {
                plugin.getLogger().warning(ex.getMessage());
            }
            new AssetPageGui(plugin).open(player, asset);
        });
    }

    public void marketSell(Player player, Asset asset, BigDecimal baseAmount) {
        if (cooldowns.onCooldown(player.getUniqueId(), plugin.getConfig().getLong("trading.cooldown_ms", 1500))) {
            plugin.send(player, "cooldown_active");
            return;
        }
        executeLocked(player, () -> {
            try {
                BigDecimal holding = plugin.balances().get(player.getUniqueId(), asset);
                if (holding.compareTo(baseAmount) < 0) {
                    plugin.send(player, "insufficient_asset", plugin.map("asset", asset.name(), "required", baseAmount.toPlainString(), "balance", holding.toPlainString()));
                    return;
                }
                BigDecimal market = plugin.prices().getPrice(asset).orElse(null);
                if (market == null) {
                    plugin.send(player, "price_unavailable", plugin.map("asset", asset.name()));
                    return;
                }
                BigDecimal bid = market.multiply(BigDecimal.ONE.subtract(percent(plugin.getConfig().getDouble("trading.spread_percent", 0.1))));
                BigDecimal gross = baseAmount.multiply(bid);
                BigDecimal fee = gross.multiply(percent(plugin.getConfig().getDouble("trading.fee_percent", 0.25))).setScale(2, RoundingMode.HALF_UP);
                BigDecimal revenue = gross.subtract(fee).setScale(2, RoundingMode.HALF_UP);
                plugin.balances().set(player.getUniqueId(), asset, holding.subtract(baseAmount));
                plugin.vault().deposit(player, revenue);
                plugin.trades().insert(player.getUniqueId(), asset, OrderSide.SELL, bid, baseAmount, revenue, fee);
                cooldowns.apply(player.getUniqueId());
                plugin.send(player, "sell_success", plugin.map("amount", baseAmount.toPlainString(), "asset", asset.name(), "revenue", revenue.toPlainString(), "fee", fee.toPlainString()));
            } catch (SQLException ex) {
                plugin.getLogger().warning(ex.getMessage());
            }
            new AssetPageGui(plugin).open(player, asset);
        });
    }

    public void placeLimit(Player player, OrderSide side, Asset asset, BigDecimal limitPrice, BigDecimal amount) {
        executeLocked(player, () -> {
            try {
                int max = plugin.getConfig().getInt("trading.max_open_orders_per_player", 25);
                if (plugin.orders().countOpen(player.getUniqueId()) >= max) {
                    plugin.send(player, "max_orders_reached", plugin.map("max", String.valueOf(max)));
                    return;
                }
                BigDecimal reservedQuote = null;
                BigDecimal reservedBase = null;
                if (side == OrderSide.BUY) {
                    reservedQuote = BigDecimalUtil.scaleUsd(amount);
                    if (!plugin.vault().withdraw(player, reservedQuote).success()) {
                        plugin.send(player, "insufficient_usd", plugin.map("required", reservedQuote.toPlainString(), "balance", plugin.vault().getBalance(player).toPlainString()));
                        return;
                    }
                } else {
                    reservedBase = BigDecimalUtil.scaleAsset(amount);
                    BigDecimal bal = plugin.balances().get(player.getUniqueId(), asset);
                    if (bal.compareTo(reservedBase) < 0) {
                        plugin.send(player, "insufficient_asset", plugin.map("asset", asset.name(), "required", reservedBase.toPlainString(), "balance", bal.toPlainString()));
                        return;
                    }
                    plugin.balances().set(player.getUniqueId(), asset, bal.subtract(reservedBase));
                }
                long id = plugin.orders().insert(new Order(-1, player.getUniqueId(), side, asset, OrderType.LIMIT, limitPrice, side == OrderSide.SELL ? amount : null, side == OrderSide.BUY ? amount : null, reservedQuote, reservedBase, System.currentTimeMillis(), OrderStatus.OPEN));
                plugin.send(player, "order_placed", plugin.map("side", side.name(), "amount", amount.toPlainString(), "asset", asset.name(), "price", limitPrice.toPlainString()));
            } catch (SQLException ex) {
                plugin.getLogger().warning(ex.getMessage());
            }
        });
    }

    public void cancelOrder(Player player, long id) {
        executeLocked(player, () -> {
            try {
                var opt = plugin.orders().findById(id);
                if (opt.isEmpty() || opt.get().status() != OrderStatus.OPEN) {
                    plugin.send(player, "order_not_found");
                    return;
                }
                Order order = opt.get();
                plugin.orders().updateStatus(id, OrderStatus.CANCELLED);
                if (order.side() == OrderSide.BUY && order.reservedQuote() != null) {
                    plugin.vault().deposit(player, order.reservedQuote());
                    plugin.send(player, "order_cancelled", plugin.map("id", String.valueOf(id), "refund_info", "$" + order.reservedQuote().toPlainString()));
                } else if (order.reservedBase() != null) {
                    BigDecimal bal = plugin.balances().get(player.getUniqueId(), order.asset());
                    plugin.balances().set(player.getUniqueId(), order.asset(), bal.add(order.reservedBase()));
                    plugin.send(player, "order_cancelled", plugin.map("id", String.valueOf(id), "refund_info", order.reservedBase().toPlainString() + " " + order.asset().name()));
                }
            } catch (SQLException ex) {
                plugin.getLogger().warning(ex.getMessage());
            }
        });
    }

    public void executeLimitFills() {
        try {
            for (Order order : plugin.orders().findAllOpen()) {
                BigDecimal market = plugin.prices().getPrice(order.asset()).orElse(null);
                if (market == null) continue;
                boolean trigger = order.side() == OrderSide.BUY ? market.compareTo(order.limitPrice()) <= 0 : market.compareTo(order.limitPrice()) >= 0;
                if (!trigger) continue;
                plugin.orders().updateStatus(order.id(), OrderStatus.FILLED);
                if (order.side() == OrderSide.BUY && order.reservedQuote() != null) {
                    BigDecimal ask = market.multiply(BigDecimal.ONE.add(percent(plugin.getConfig().getDouble("trading.spread_percent", 0.1))));
                    BigDecimal base = order.reservedQuote().divide(ask, 8, RoundingMode.HALF_UP);
                    BigDecimal bal = plugin.balances().get(order.uuid(), order.asset());
                    plugin.balances().set(order.uuid(), order.asset(), bal.add(base));
                    plugin.trades().insert(order.uuid(), order.asset(), OrderSide.BUY, ask, base, order.reservedQuote(), BigDecimal.ZERO);
                    notifyFill(order, base, ask);
                } else if (order.side() == OrderSide.SELL && order.reservedBase() != null) {
                    BigDecimal bid = market.multiply(BigDecimal.ONE.subtract(percent(plugin.getConfig().getDouble("trading.spread_percent", 0.1))));
                    BigDecimal quote = order.reservedBase().multiply(bid).setScale(2, RoundingMode.HALF_UP);
                    Player online = Bukkit.getPlayer(order.uuid());
                    if (online != null) {
                        plugin.vault().deposit(online, quote);
                    }
                    plugin.trades().insert(order.uuid(), order.asset(), OrderSide.SELL, bid, order.reservedBase(), quote, BigDecimal.ZERO);
                    notifyFill(order, order.reservedBase(), bid);
                }
            }
        } catch (SQLException ex) {
            plugin.getLogger().warning(ex.getMessage());
        }
    }

    private void notifyFill(Order order, BigDecimal amount, BigDecimal price) {
        Player online = Bukkit.getPlayer(order.uuid());
        String msg = plugin.msg("order_filled", plugin.map("side", order.side().name(), "amount", amount.toPlainString(), "asset", order.asset().name(), "price", price.toPlainString()));
        if (online != null) {
            online.sendMessage(msg);
        } else {
            offlineQueue.put(order.uuid(), msg);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        String queued = offlineQueue.remove(event.getPlayer().getUniqueId());
        if (queued != null) {
            event.getPlayer().sendMessage(queued);
        }
    }

    private boolean preTrade(Player player, BigDecimal usdAmount, boolean minCheck, Asset asset) {
        if (cooldowns.onCooldown(player.getUniqueId(), plugin.getConfig().getLong("trading.cooldown_ms", 1500))) {
            plugin.send(player, "cooldown_active");
            return false;
        }
        if (minCheck && usdAmount.compareTo(new BigDecimal(plugin.getConfig().getString("trading.min_usd", "1.00"))) < 0) {
            plugin.send(player, "min_usd_error", plugin.map("min_usd", plugin.getConfig().getString("trading.min_usd", "1.00")));
            return false;
        }
        if (plugin.prices().getPrice(asset).isEmpty()) {
            plugin.send(player, "price_unavailable", plugin.map("asset", asset.name()));
            return false;
        }
        return true;
    }

    private void executeLocked(Player player, Runnable action) {
        if (locks.putIfAbsent(player.getUniqueId(), new Object()) != null) {
            plugin.send(player, "trade_locked");
            return;
        }
        TaskUtil.runAsync(plugin, () -> {
            try {
                action.run();
            } finally {
                locks.remove(player.getUniqueId());
            }
        });
    }

    private BigDecimal percent(double val) {
        return BigDecimal.valueOf(val).divide(BigDecimal.valueOf(100), 8, RoundingMode.HALF_UP);
    }
}
