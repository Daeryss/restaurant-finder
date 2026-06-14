package com.bonial.restaurantfinder.exception;

/**
 * Raised when the client supplies an invalid request: a location outside the city boundaries, an
 * unknown sort value, and similar input errors. Maps to HTTP 400. The message carries the specifics.
 */
public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) {
        super(message);
    }
}
