package com.bonial.restaurantfinder.service;

import com.bonial.restaurantfinder.domain.Coordinate;
import com.bonial.restaurantfinder.domain.Restaurant;
import com.bonial.restaurantfinder.exception.RestaurantNotFoundException;
import com.bonial.restaurantfinder.repository.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

/**
 * Core use cases of the application: finding the restaurants visible from a client location and
 * looking up a restaurant by id.
 *
 * <p>"Visible" means the client falls within the restaurant's bounding circle, i.e. the distance
 * from the client to the restaurant is no greater than the restaurant's radius {@code B(x,y) = x}.
 */
@Service
public class RestaurantService {

    private final RestaurantRepository repository;

    public RestaurantService(RestaurantRepository repository) {
        this.repository = repository;
    }

    /**
     * Restaurants whose bounding circle covers {@code clientLocation}, each paired with its distance
     * from the client and ordered as requested.
     *
     * <p>The scan is linear in the number of restaurants. The catalogue is tiny and bounded by the
     * grid, so a straight filter is the clearest correct approach; a spatial index would only earn
     * its complexity at a far larger scale.
     */
    public List<RankedRestaurant> findVisibleRestaurants(Coordinate clientLocation, SortOrder sortOrder) {
        Comparator<RankedRestaurant> byDistance = Comparator
                .comparingDouble(RankedRestaurant::distance)
                .thenComparingInt(ranked -> ranked.restaurant().coordinate().x())
                .thenComparingInt(ranked -> ranked.restaurant().coordinate().y())
                .thenComparing(ranked -> ranked.restaurant().id());
        if (sortOrder == SortOrder.DESC) {
            byDistance = byDistance.reversed();
        }
        return repository.findAll().stream()
                .filter(restaurant -> restaurant.isVisibleFrom(clientLocation))
                .map(restaurant -> new RankedRestaurant(restaurant, restaurant.distanceFrom(clientLocation)))
                .sorted(byDistance)
                .toList();
    }

    /**
     * The restaurant with the given id.
     *
     * @throws RestaurantNotFoundException if no restaurant has that id
     */
    public Restaurant getById(String id) {
        return repository.findById(id)
                .orElseThrow(() -> new RestaurantNotFoundException(id));
    }
}
