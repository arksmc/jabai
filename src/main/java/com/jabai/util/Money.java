package com.jabai.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Money {
    private Money() {
    }

    /** Parses "42" or "42.50" into 4200 / 4250 centavos. Throws NumberFormatException on bad input. */
    public static long toCentavos(String text) {
        BigDecimal value = new BigDecimal(text.trim());
        return value.setScale(2, RoundingMode.HALF_UP).movePointRight(2).longValueExact();
    }

    public static String format(long centavos) {
        return String.format("₱%,.2f", BigDecimal.valueOf(centavos, 2));
    }
}