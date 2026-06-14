package com.bonial.restaurantfinder.web.error;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Uniform error body returned for every failed request, so clients can rely on a single shape.
 *
 * @param status HTTP status code
 * @param error  short reason phrase, e.g. {@code "Not Found"}
 * @param message human-readable explanation
 * @param path   request path that produced the error
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(int status, String error, String message, String path) {
}
