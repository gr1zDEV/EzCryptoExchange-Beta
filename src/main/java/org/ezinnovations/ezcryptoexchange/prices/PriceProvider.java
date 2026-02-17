package org.ezinnovations.ezcryptoexchange.prices;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface PriceProvider {
    CompletableFuture<Map<Asset, BigDecimal>> fetchPrices();
}
