package com.bonial.restaurantfinder.web.dto;

import com.bonial.restaurantfinder.service.DistanceFormatter;
import com.bonial.restaurantfinder.service.RankedRestaurant;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * One entry in the {@code /search_locations} response: a restaurant in view together with the
 * client's distance to it. Field order and the string-typed {@code distance} mirror the format
 * given in the challenge.
 */
@JsonPropertyOrder({"id", "name", "coordinate", "distance"})
public record LocationView(String id, String name, String coordinate, String distance) {

    public static LocationView from(RankedRestaurant ranked) {
        var restaurant = ranked.restaurant();
        return new LocationView(
                restaurant.id(),
                restaurant.name(),
                restaurant.coordinate().asText(),
                DistanceFormatter.format(ranked.distance()));
    }
}
