package org.ezinnovations.ezcryptoexchange.storage;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;

public final class WalletRepository {
    private final DatabaseManager databaseManager;

    public WalletRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public String ensureWallet(UUID uuid, String name) throws SQLException {
        Optional<String> existing = findWalletId(uuid);
        if (existing.isPresent()) {
            updateName(uuid, name);
            return existing.get();
        }
        String id = "EZ-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
        databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO wallets(uuid,wallet_id,created_at,last_seen_name) VALUES(?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, id);
                ps.setLong(3, System.currentTimeMillis());
                ps.setString(4, name);
                ps.executeUpdate();
            }
            return null;
        });
        return id;
    }

    public Optional<String> findWalletId(UUID uuid) throws SQLException {
        return databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT wallet_id FROM wallets WHERE uuid=?")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return Optional.of(rs.getString(1));
                    }
                    return Optional.empty();
                }
            }
        });
    }

    private void updateName(UUID uuid, String name) throws SQLException {
        databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE wallets SET last_seen_name=? WHERE uuid=?")) {
                ps.setString(1, name);
                ps.setString(2, uuid.toString());
                ps.executeUpdate();
            }
            return null;
        });
    }
}
