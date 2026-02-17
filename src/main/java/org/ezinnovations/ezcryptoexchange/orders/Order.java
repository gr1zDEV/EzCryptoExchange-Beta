package org.ezinnovations.ezcryptoexchange.orders;

import org.ezinnovations.ezcryptoexchange.prices.Asset;

import java.math.BigDecimal;
import java.util.UUID;

public record Order(long id, UUID uuid, OrderSide side, Asset asset, OrderType type, BigDecimal limitPrice,
                    BigDecimal baseAmount, BigDecimal quoteAmount, BigDecimal reservedQuote, BigDecimal reservedBase,
                    long createdAt, OrderStatus status) {
}
