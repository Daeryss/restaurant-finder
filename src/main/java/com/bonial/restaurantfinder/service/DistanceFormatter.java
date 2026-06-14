package com.bonial.restaurantfinder.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Formats a distance for the API exactly as the challenge specifies: up to six decimal places,
 * with trailing zeros trimmed so whole numbers render as {@code "5"} rather than {@code "5.000000"}
 * (e.g. {@code 3.6055512} → {@code "3.605551"}, {@code 5.0} → {@code "5"}).
 */
public final class DistanceFormatter {

    private static final int SCALE = 6;

    private DistanceFormatter() {
    }

    public static String format(double distance) {
        return BigDecimal.valueOf(distance)
                .setScale(SCALE, RoundingMode.HALF_UP)
                .stripTrailingZeros()
                .toPlainString();
    }
}
