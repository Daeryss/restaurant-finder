package com.bonial.restaurantfinder.repository;

import com.bonial.restaurantfinder.domain.Restaurant;

import java.util.List;
import java.util.Optional;

/**
 * Access to the restaurant catalogue.
 *
 * <p>Defined as an interface so the storage mechanism can evolve without touching the service or
 * web layers. Today it is backed by JSON files on the classpath ({@link JsonRestaurantRepository});
 * a database- or HTTP-backed implementation could be dropped in later behind the same contract.
 */
public interface RestaurantRepository {

    /** All restaurants in the catalogue. */
    List<Restaurant> findAll();

    /** The restaurant with the given id, if present. */
    Optional<Restaurant> findById(String id);
}
