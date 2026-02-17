package org.ezinnovations.ezcryptoexchange;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.ezinnovations.ezcryptoexchange.commands.CryptoCommand;
import org.ezinnovations.ezcryptoexchange.commands.CryptoTabCompleter;
import org.ezinnovations.ezcryptoexchange.economy.VaultHook;
import org.ezinnovations.ezcryptoexchange.gui.GuiListener;
import org.ezinnovations.ezcryptoexchange.input.InputListener;
import org.ezinnovations.ezcryptoexchange.input.InputManager;
import org.ezinnovations.ezcryptoexchange.orders.LimitOrderExecutor;
import org.ezinnovations.ezcryptoexchange.orders.OrderManager;
import org.ezinnovations.ezcryptoexchange.prices.CachedPriceService;
import org.ezinnovations.ezcryptoexchange.prices.CoinGeckoPriceProvider;
import org.ezinnovations.ezcryptoexchange.storage.*;
import org.ezinnovations.ezcryptoexchange.util.ColorUtil;

import java.io.File;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public final class EZCryptoExchange extends JavaPlugin {
    private FileConfiguration messages;
    private DatabaseManager databaseManager;
    private WalletRepository walletRepository;
    private BalanceRepository balanceRepository;
    private OrderRepository orderRepository;
    private TradeRepository tradeRepository;
    private VaultHook vaultHook;
    private CachedPriceService priceService;
    private InputManager inputManager;
    private OrderManager orderManager;
    private LimitOrderExecutor limitOrderExecutor;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveResource("messages.yml", false);
        reloadMessages();

        databaseManager = new DatabaseManager(this);
        try {
            databaseManager.init();
        } catch (SQLException ex) {
            getLogger().severe("Database init failed: " + ex.getMessage());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        walletRepository = new WalletRepository(databaseManager);
        balanceRepository = new BalanceRepository(databaseManager);
        orderRepository = new OrderRepository(databaseManager);
        tradeRepository = new TradeRepository(databaseManager);

        vaultHook = new VaultHook(this);
        vaultHook.resolve();
        if (!vaultHook.isAvailable()) {
            getLogger().info("Vault economy not found. Trading actions are disabled.");
        }

        priceService = new CachedPriceService(this, new CoinGeckoPriceProvider());
        priceService.start(getConfig().getInt("price_refresh_seconds", 15));

        inputManager = new InputManager(this);
        orderManager = new OrderManager(this);
        limitOrderExecutor = new LimitOrderExecutor(this, orderManager);
        limitOrderExecutor.start(getConfig().getInt("limit_check_interval_seconds", 5));

        CryptoCommand command = new CryptoCommand(this);
        if (getCommand("crypto") != null) {
            getCommand("crypto").setExecutor(command);
            getCommand("crypto").setTabCompleter(new CryptoTabCompleter());
        }
        Bukkit.getPluginManager().registerEvents(new GuiListener(this), this);
        Bukkit.getPluginManager().registerEvents(new InputListener(this), this);
        Bukkit.getPluginManager().registerEvents(orderManager, this);
    }

    @Override
    public void onDisable() {
        if (priceService != null) {
            priceService.stop();
        }
        if (limitOrderExecutor != null) {
            limitOrderExecutor.stop();
        }
        Bukkit.getScheduler().cancelTasks(this);
        if (inputManager != null) {
            inputManager.clearAll();
        }
        if (databaseManager != null) {
            databaseManager.shutdown();
        }
    }

    public void reloadPlugin() {
        reloadConfig();
        reloadMessages();
    }

    private void reloadMessages() {
        File file = new File(getDataFolder(), "messages.yml");
        messages = YamlConfiguration.loadConfiguration(file);
    }

    public String msg(String key) {
        return msg(key, Map.of());
    }

    public String msg(String key, Map<String, String> placeholders) {
        String text = messages.getString(key, key);
        String prefix = messages.getString("prefix", "");
        text = text.replace("{prefix}", prefix);
        for (Map.Entry<String, String> e : placeholders.entrySet()) {
            text = text.replace("{" + e.getKey() + "}", e.getValue());
        }
        return ColorUtil.colorize(text);
    }

    public void send(Player player, String key) {
        player.sendMessage(msg(key));
    }

    public void send(Player player, String key, Map<String, String> placeholders) {
        player.sendMessage(msg(key, placeholders));
    }

    public Map<String, String> map(String... values) {
        Map<String, String> result = new HashMap<>();
        for (int i = 0; i < values.length - 1; i += 2) {
            result.put(values[i], values[i + 1]);
        }
        return result;
    }

    public FileConfiguration messages() { return messages; }
    public WalletRepository wallets() { return walletRepository; }
    public BalanceRepository balances() { return balanceRepository; }
    public OrderRepository orders() { return orderRepository; }
    public TradeRepository trades() { return tradeRepository; }
    public VaultHook vault() { return vaultHook; }
    public CachedPriceService prices() { return priceService; }
    public InputManager inputs() { return inputManager; }
    public OrderManager orderManager() { return orderManager; }
}
