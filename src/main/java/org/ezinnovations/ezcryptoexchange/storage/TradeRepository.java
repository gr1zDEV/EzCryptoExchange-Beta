package org.ezinnovations.ezcryptoexchange.storage;

import org.ezinnovations.ezcryptoexchange.orders.OrderSide;
import org.ezinnovations.ezcryptoexchange.prices.Asset;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.UUID;

public final class TradeRepository {
    private final DatabaseManager databaseManager;

    public TradeRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public void insert(UUID uuid, Asset asset, OrderSide side, BigDecimal price, BigDecimal baseAmount, BigDecimal quoteAmount, BigDecimal fee) throws SQLException {
        databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO trades(uuid,asset,side,price,base_amount,quote_amount,fee,timestamp) VALUES(?,?,?,?,?,?,?,?)")) {
                ps.setString(1, uuid.toString());
                ps.setString(2, asset.name());
                ps.setString(3, side.name());
                ps.setString(4, price.toPlainString());
                ps.setString(5, baseAmount.toPlainString());
                ps.setString(6, quoteAmount.toPlainString());
                ps.setString(7, fee.toPlainString());
                ps.setLong(8, System.currentTimeMillis());
                ps.executeUpdate();
            }
            return null;
        });
    }
}
