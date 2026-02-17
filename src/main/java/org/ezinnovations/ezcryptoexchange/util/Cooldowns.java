package org.ezinnovations.ezcryptoexchange.util;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class Cooldowns {
    private final Map<UUID, Long> cooldowns = new ConcurrentHashMap<>();

    public boolean onCooldown(UUID uuid, long cooldownMs) {
        Long last = cooldowns.get(uuid);
        return last != null && (System.currentTimeMillis() - last) < cooldownMs;
    }

    public void apply(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis());
    }

    public void clear() {
        cooldowns.clear();
    }
}
