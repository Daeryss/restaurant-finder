package com.bonial.restaurantfinder.web.dto;

import com.bonial.restaurantfinder.domain.Restaurant;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * Response of {@code GET /location/{id}}: the full detail view of a single restaurant.
 *
 * <p>The {@code openningHours} key keeps the (mis)spelling used in the challenge's data files and
 * sample response so existing clients are not broken.
 */
@JsonPropertyOrder({"name", "type", "id", "openningHours", "image", "coordinate"})
public record RestaurantDetail(
        String name,
        String type,
        String id,
        @JsonProperty("openningHours") String openingHours,
        String image,
        String coordinate) {

    public static RestaurantDetail from(Restaurant restaurant) {
        return new RestaurantDetail(
                restaurant.name(),
                restaurant.type(),
                restaurant.id(),
                restaurant.openingHours(),
                restaurant.image(),
                restaurant.coordinate().asText());
    }
}
