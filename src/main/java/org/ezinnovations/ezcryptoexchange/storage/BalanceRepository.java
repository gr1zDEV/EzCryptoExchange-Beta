package org.ezinnovations.ezcryptoexchange.storage;

import org.ezinnovations.ezcryptoexchange.prices.Asset;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public final class BalanceRepository {
    private final DatabaseManager databaseManager;

    public BalanceRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public BigDecimal get(UUID uuid, Asset asset) throws SQLException {
        return databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT amount FROM balances WHERE uuid=? AND asset=?")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, asset.name());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? new BigDecimal(rs.getString(1)) : BigDecimal.ZERO;
                }
            }
        });
    }

    public Map<Asset, BigDecimal> getAll(UUID uuid) throws SQLException {
        Map<Asset, BigDecimal> map = new EnumMap<>(Asset.class);
        for (Asset asset : Asset.values()) {
            map.put(asset, get(uuid, asset));
        }
        return map;
    }

    public void set(UUID uuid, Asset asset, BigDecimal amount) throws SQLException {
        databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO balances(uuid,asset,amount) VALUES(?,?,?) ON CONFLICT(uuid, asset) DO UPDATE SET amount=excluded.amount")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, asset.name());
                ps.setString(3, amount.toPlainString());
                ps.executeUpdate();
            }
            return null;
        });
    }
}
