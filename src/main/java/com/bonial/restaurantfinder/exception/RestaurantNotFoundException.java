package com.bonial.restaurantfinder.exception;

/** Raised when a restaurant id does not match any restaurant in the catalogue. Maps to HTTP 404. */
public class RestaurantNotFoundException extends RuntimeException {

    public RestaurantNotFoundException(String id) {
        super("No restaurant found with id '" + id + "'");
    }
}
