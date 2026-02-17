package org.ezinnovations.ezcryptoexchange.prices;

import org.bukkit.Material;

public enum Asset {
    BTC("Bitcoin", "bitcoin", 8, Material.GOLD_BLOCK),
    ETH("Ethereum", "ethereum", 8, Material.DIAMOND_BLOCK),
    SOL("Solana", "solana", 8, Material.EMERALD_BLOCK);

    private final String displayName;
    private final String coingeckoId;
    private final int decimals;
    private final Material material;

    Asset(String displayName, String coingeckoId, int decimals, Material material) {
        this.displayName = displayName;
        this.coingeckoId = coingeckoId;
        this.decimals = decimals;
        this.material = material;
    }

    public String displayName() { return displayName; }
    public String coingeckoId() { return coingeckoId; }
    public int decimals() { return decimals; }
    public Material material() { return material; }
}
