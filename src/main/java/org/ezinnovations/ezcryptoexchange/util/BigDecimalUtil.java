package org.ezinnovations.ezcryptoexchange.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.util.Optional;

public final class BigDecimalUtil {
    private BigDecimalUtil() {
    }

    public static BigDecimal scaleUsd(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    public static BigDecimal scaleAsset(BigDecimal value) {
        return value.setScale(8, RoundingMode.HALF_UP);
    }

    public static Optional<BigDecimal> parse(String raw) {
        try {
            return Optional.of(new BigDecimal(raw.trim()));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    public static String format(BigDecimal value, String pattern) {
        DecimalFormat format = new DecimalFormat(pattern);
        return format.format(value);
    }
}
