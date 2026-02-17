package org.ezinnovations.ezcryptoexchange.storage;

import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.locks.ReentrantLock;

public final class DatabaseManager {
    private final JavaPlugin plugin;
    private final ReentrantLock lock = new ReentrantLock();
    private Connection connection;

    public DatabaseManager(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void init() throws SQLException {
        File folder = new File(plugin.getDataFolder().getParentFile(), "EZCryptoExchange");
        if (!folder.exists()) {
            folder.mkdirs();
        }
        connection = DriverManager.getConnection("jdbc:sqlite:" + new File(folder, "data.db").getAbsolutePath());
        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS wallets (uuid TEXT PRIMARY KEY,wallet_id TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL,last_seen_name TEXT)");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS balances (uuid TEXT NOT NULL,asset TEXT NOT NULL,amount TEXT NOT NULL DEFAULT '0',PRIMARY KEY (uuid, asset))");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS orders (id INTEGER PRIMARY KEY AUTOINCREMENT,uuid TEXT NOT NULL,side TEXT NOT NULL,asset TEXT NOT NULL,type TEXT NOT NULL,limit_price TEXT,base_amount TEXT,quote_amount TEXT,reserved_quote TEXT,reserved_base TEXT,created_at INTEGER NOT NULL,status TEXT NOT NULL DEFAULT 'OPEN')");
            statement.executeUpdate("CREATE TABLE IF NOT EXISTS trades (id INTEGER PRIMARY KEY AUTOINCREMENT,uuid TEXT NOT NULL,asset TEXT NOT NULL,side TEXT NOT NULL,price TEXT NOT NULL,base_amount TEXT NOT NULL,quote_amount TEXT NOT NULL,fee TEXT NOT NULL,timestamp INTEGER NOT NULL)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_uuid_status ON orders(uuid, status)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status)");
            statement.executeUpdate("CREATE INDEX IF NOT EXISTS idx_trades_uuid ON trades(uuid)");
        }
    }

    public <T> T withLock(SqlSupplier<T> supplier) throws SQLException {
        lock.lock();
        try {
            return supplier.get(connection);
        } finally {
            lock.unlock();
        }
    }

    public void shutdown() {
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException ex) {
                plugin.getLogger().warning("Failed to close db: " + ex.getMessage());
            }
        }
    }

    @FunctionalInterface
    public interface SqlSupplier<T> {
        T get(Connection connection) throws SQLException;
    }
}
