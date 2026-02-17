package org.ezinnovations.ezcryptoexchange.storage;

import org.ezinnovations.ezcryptoexchange.orders.Order;
import org.ezinnovations.ezcryptoexchange.orders.OrderSide;
import org.ezinnovations.ezcryptoexchange.orders.OrderStatus;
import org.ezinnovations.ezcryptoexchange.orders.OrderType;
import org.ezinnovations.ezcryptoexchange.prices.Asset;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public final class OrderRepository {
    private final DatabaseManager databaseManager;

    public OrderRepository(DatabaseManager databaseManager) {
        this.databaseManager = databaseManager;
    }

    public long insert(Order order) throws SQLException {
        return databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("INSERT INTO orders(uuid,side,asset,type,limit_price,base_amount,quote_amount,reserved_quote,reserved_base,created_at,status) VALUES(?,?,?,?,?,?,?,?,?,?,?)", PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, order.uuid().toString());
                ps.setString(2, order.side().name());
                ps.setString(3, order.asset().name());
                ps.setString(4, order.type().name());
                ps.setString(5, toString(order.limitPrice()));
                ps.setString(6, toString(order.baseAmount()));
                ps.setString(7, toString(order.quoteAmount()));
                ps.setString(8, toString(order.reservedQuote()));
                ps.setString(9, toString(order.reservedBase()));
                ps.setLong(10, order.createdAt());
                ps.setString(11, order.status().name());
                ps.executeUpdate();
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    return rs.next() ? rs.getLong(1) : -1L;
                }
            }
        });
    }

    public List<Order> findOpen(UUID uuid, int limit, int offset) throws SQLException {
        return databaseManager.withLock(connection -> {
            List<Order> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM orders WHERE uuid=? AND status='OPEN' ORDER BY created_at DESC LIMIT ? OFFSET ?")) {
                ps.setString(1, uuid.toString());
                ps.setInt(2, limit);
                ps.setInt(3, offset);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
            return list;
        });
    }

    public List<Order> findAllOpen() throws SQLException {
        return databaseManager.withLock(connection -> {
            List<Order> list = new ArrayList<>();
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM orders WHERE status='OPEN'")) {
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        list.add(map(rs));
                    }
                }
            }
            return list;
        });
    }

    public Optional<Order> findById(long id) throws SQLException {
        return databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT * FROM orders WHERE id=?")) {
                ps.setLong(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? Optional.of(map(rs)) : Optional.empty();
                }
            }
        });
    }

    public void updateStatus(long id, OrderStatus status) throws SQLException {
        databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE orders SET status=? WHERE id=?")) {
                ps.setString(1, status.name());
                ps.setLong(2, id);
                ps.executeUpdate();
            }
            return null;
        });
    }

    public int countOpen(UUID uuid) throws SQLException {
        return databaseManager.withLock(connection -> {
            try (PreparedStatement ps = connection.prepareStatement("SELECT COUNT(*) FROM orders WHERE uuid=? AND status='OPEN'")) {
                ps.setString(1, uuid.toString());
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getInt(1) : 0;
                }
            }
        });
    }

    private Order map(ResultSet rs) throws SQLException {
        return new Order(
            rs.getLong("id"),
            UUID.fromString(rs.getString("uuid")),
            OrderSide.valueOf(rs.getString("side")),
            Asset.valueOf(rs.getString("asset")),
            OrderType.valueOf(rs.getString("type")),
            parse(rs.getString("limit_price")),
            parse(rs.getString("base_amount")),
            parse(rs.getString("quote_amount")),
            parse(rs.getString("reserved_quote")),
            parse(rs.getString("reserved_base")),
            rs.getLong("created_at"),
            OrderStatus.valueOf(rs.getString("status"))
        );
    }

    private BigDecimal parse(String value) { return value == null ? null : new BigDecimal(value); }
    private String toString(BigDecimal value) { return value == null ? null : value.toPlainString(); }
}
