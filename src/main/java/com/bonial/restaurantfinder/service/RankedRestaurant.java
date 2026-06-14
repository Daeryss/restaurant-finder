package com.bonial.restaurantfinder.service;

import com.bonial.restaurantfinder.domain.Restaurant;

/** A restaurant paired with its computed distance from the client who searched. */
public record RankedRestaurant(Restaurant restaurant, double distance) {
}
