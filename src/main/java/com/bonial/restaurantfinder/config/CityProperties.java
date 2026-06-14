package com.bonial.restaurantfinder.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Boundaries of the city center, externalised so the grid can be resized without code changes.
 *
 * <p>Defaults match the challenge: {@code max(x) = 14}, {@code max(y) = 10}. The minimum is 0
 * (the city square sits at the origin).
 */
@ConfigurationProperties(prefix = "city")
public record CityProperties(int maxX, int maxY) {

    public CityProperties {
        if (maxX < 0 || maxY < 0) {
            throw new IllegalArgumentException("City boundaries must be non-negative");
        }
    }

    public boolean contains(int x, int y) {
        return x >= 0 && x <= maxX && y >= 0 && y <= maxY;
    }
}
