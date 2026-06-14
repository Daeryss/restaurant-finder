package com.bonial.restaurantfinder.domain;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * A restaurant as stored in the JSON data files.
 *
 * <p>Restaurants sit on the {@code x == y} diagonal of the grid (the city square at {@code (0,0)}
 * excluded). Per the challenge, each restaurant's visibility radius equals its grid number, i.e.
 * the {@code x} value of its coordinate: {@code B(x,y) = x}.
 *
 * <p>The data files are slightly inconsistent — some name the restaurant with a {@code "name"} key
 * and others with {@code "title"} — so we accept both via {@link JsonAlias}. Unknown keys are
 * ignored to keep loading resilient to future fields.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Restaurant(
        String id,
        @JsonAlias({"title"}) String name,
        String type,
        @JsonProperty("openningHours") String openingHours,
        String image,
        @JsonDeserialize(using = CoordinateDeserializer.class) Coordinate coordinate) {

    /** Visibility radius of this restaurant: {@code B(x,y) = x}. */
    public int visibilityRadius() {
        return coordinate.x();
    }

    /** A client at {@code clientLocation} can see this restaurant when they fall within its radius. */
    public boolean isVisibleFrom(Coordinate clientLocation) {
        return coordinate.distanceTo(clientLocation) <= visibilityRadius();
    }

    /** Distance from the given client location to this restaurant. */
    public double distanceFrom(Coordinate clientLocation) {
        return coordinate.distanceTo(clientLocation);
    }
}
