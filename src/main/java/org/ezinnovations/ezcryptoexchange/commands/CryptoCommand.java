package org.ezinnovations.ezcryptoexchange.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.ezinnovations.ezcryptoexchange.EZCryptoExchange;
import org.ezinnovations.ezcryptoexchange.gui.MainExchangeGui;
import org.ezinnovations.ezcryptoexchange.gui.OrdersGui;
import org.ezinnovations.ezcryptoexchange.prices.Asset;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.Map;

public final class CryptoCommand implements CommandExecutor {
    private final EZCryptoExchange plugin;

    public CryptoCommand(EZCryptoExchange plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player_only"));
            return true;
        }
        if (!player.hasPermission("ezcrypto.use")) {
            player.sendMessage(plugin.msg("no_permission"));
            return true;
        }
        if (args.length == 0) {
            new MainExchangeGui(plugin).open(player);
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "wallet" -> wallet(player);
            case "orders" -> new OrdersGui(plugin).open(player, 1);
            case "reload" -> {
                if (!player.hasPermission("ezcrypto.admin")) {
                    player.sendMessage(plugin.msg("no_permission"));
                    return true;
                }
                plugin.reloadPlugin();
                player.sendMessage(plugin.msg("reload_success"));
            }
            default -> new MainExchangeGui(plugin).open(player);
        }
        return true;
    }

    private void wallet(Player player) {
        try {
            String id = plugin.wallets().ensureWallet(player.getUniqueId(), player.getName());
            player.sendMessage(plugin.msg("wallet_header"));
            player.sendMessage(plugin.msg("wallet_id", Map.of("wallet_id", id)));
            for (Asset asset : Asset.values()) {
                BigDecimal amount = plugin.balances().get(player.getUniqueId(), asset);
                BigDecimal price = plugin.prices().getPrice(asset).orElse(BigDecimal.ZERO);
                BigDecimal value = amount.multiply(price);
                player.sendMessage(plugin.msg("wallet_balance", plugin.map("asset", asset.name(), "amount", amount.toPlainString(), "value", value.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
            }
            player.sendMessage(plugin.msg("wallet_usd", plugin.map("balance", plugin.vault().getBalance(player).setScale(2, java.math.RoundingMode.HALF_UP).toPlainString())));
            player.sendMessage(plugin.msg("wallet_footer"));
        } catch (SQLException ex) {
            plugin.getLogger().warning(ex.getMessage());
        }
    }
}
