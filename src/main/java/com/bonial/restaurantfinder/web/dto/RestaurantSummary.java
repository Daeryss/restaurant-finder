package com.bonial.restaurantfinder.web.dto;

import com.bonial.restaurantfinder.domain.Restaurant;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Lightweight view of a restaurant for the catalogue listing ({@code GET /locations}): just enough
 * to place it on the map and link to its detail, without a client-specific distance.
 */
@JsonPropertyOrder({"id", "name", "coordinate"})
public record RestaurantSummary(String id, String name, String coordinate) {

    public static RestaurantSummary from(Restaurant restaurant) {
        return new RestaurantSummary(restaurant.id(), restaurant.name(), restaurant.coordinate().asText());
    }
}
