package com.bonial.restaurantfinder.web.dto;

import com.bonial.restaurantfinder.domain.Coordinate;
import com.bonial.restaurantfinder.service.RankedRestaurant;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

/**
 * Response of {@code GET /search_locations}: the client's own location echoed back, plus the list
 * of restaurants currently in view.
 */
@JsonPropertyOrder({"user-location", "locations"})
public record SearchResponse(
        @JsonProperty("user-location") String userLocation,
        List<LocationView> locations) {

    public static SearchResponse of(Coordinate clientLocation, List<RankedRestaurant> visible) {
        List<LocationView> views = visible.stream()
                .map(LocationView::from)
                .toList();
        return new SearchResponse(clientLocation.asText(), views);
    }
}
