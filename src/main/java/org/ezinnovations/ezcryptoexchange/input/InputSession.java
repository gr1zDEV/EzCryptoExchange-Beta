package org.ezinnovations.ezcryptoexchange.input;

import org.bukkit.scheduler.BukkitTask;

import java.util.UUID;
import java.util.function.Consumer;

public record InputSession(UUID uuid, Consumer<String> callback, InputType inputType, BukkitTask timeoutTask) {
    public enum InputType {
        CUSTOM_BUY,
        CUSTOM_SELL,
        LIMIT_PRICE,
        LIMIT_AMOUNT
    }
}
