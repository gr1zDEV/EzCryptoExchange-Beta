package org.ezinnovations.ezcryptoexchange.prices;

import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

public final class CoinGeckoPriceProvider implements PriceProvider {
    private static final String URL = "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum,solana&vs_currencies=usd";
    private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    @Override
    public CompletableFuture<Map<Asset, BigDecimal>> fetchPrices() {
        HttpRequest request = HttpRequest.newBuilder(URI.create(URL)).timeout(Duration.ofSeconds(10)).GET().build();
        return client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenApply(this::parseJson);
    }

    private Map<Asset, BigDecimal> parseJson(String body) {
        Map<Asset, BigDecimal> map = new EnumMap<>(Asset.class);
        map.put(Asset.BTC, read(body, "bitcoin"));
        map.put(Asset.ETH, read(body, "ethereum"));
        map.put(Asset.SOL, read(body, "solana"));
        return map;
    }

    private BigDecimal read(String body, String id) {
        String key = "\"" + id + "\":{\"usd\":";
        int start = body.indexOf(key);
        if (start == -1) {
            throw new IllegalStateException("Missing id " + id);
        }
        int valueStart = start + key.length();
        int end = valueStart;
        while (end < body.length() && "0123456789.".indexOf(body.charAt(end)) >= 0) {
            end++;
        }
        return new BigDecimal(body.substring(valueStart, end));
    }
}
