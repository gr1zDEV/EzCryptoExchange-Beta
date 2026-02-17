package org.ezinnovations.ezcryptoexchange.economy;

import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.math.BigDecimal;

public final class VaultHook {
    private final JavaPlugin plugin;
    private @Nullable Economy economy;

    public VaultHook(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void resolve() {
        if (plugin.getServer().getPluginManager().getPlugin("Vault") == null) {
            economy = null;
            return;
        }
        RegisteredServiceProvider<Economy> rsp = plugin.getServer().getServicesManager().getRegistration(Economy.class);
        economy = rsp == null ? null : rsp.getProvider();
    }

    public boolean isAvailable() {
        return economy != null;
    }

    public BigDecimal getBalance(Player player) {
        if (economy == null) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(economy.getBalance(player));
    }

    public TransactionResult withdraw(Player player, BigDecimal amount) {
        if (economy == null) {
            return TransactionResult.failed("Vault missing");
        }
        EconomyResponse response = economy.withdrawPlayer(player, amount.doubleValue());
        if (response.type == EconomyResponse.ResponseType.SUCCESS) {
            return TransactionResult.ok();
        }
        if (response.type == EconomyResponse.ResponseType.FAILURE && response.errorMessage != null && response.errorMessage.toLowerCase().contains("insufficient")) {
            return TransactionResult.insufficient(response.errorMessage);
        }
        return TransactionResult.failed(response.errorMessage == null ? "Failed" : response.errorMessage);
    }

    public TransactionResult deposit(Player player, BigDecimal amount) {
        if (economy == null) {
            return TransactionResult.failed("Vault missing");
        }
        EconomyResponse response = economy.depositPlayer(player, amount.doubleValue());
        if (response.type == EconomyResponse.ResponseType.SUCCESS) {
            return TransactionResult.ok();
        }
        return TransactionResult.failed(response.errorMessage == null ? "Failed" : response.errorMessage);
    }
}
