package com.bonial.restaurantfinder.repository;

import com.bonial.restaurantfinder.domain.Restaurant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Repository;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Loads the restaurant catalogue from JSON files on the classpath once at startup and serves it
 * from memory.
 *
 * <p>The dataset is tiny and effectively static, so an in-memory map keyed by id gives O(1) lookups
 * and trivially correct behaviour without a database. The {@code findAll}/{@code findById} contract
 * is the seam to swap in a persistent store if the catalogue ever grows.
 */
@Repository
public class JsonRestaurantRepository implements RestaurantRepository {

    private static final Logger log = LoggerFactory.getLogger(JsonRestaurantRepository.class);

    private final ObjectMapper objectMapper;
    private final String locationPattern;

    /** Insertion-ordered so {@code findAll} returns a stable, predictable ordering. */
    private final Map<String, Restaurant> byId = new LinkedHashMap<>();

    public JsonRestaurantRepository(
            ObjectMapper objectMapper,
            @Value("${restaurants.location:classpath:restaurants/*.json}") String locationPattern) {
        this.objectMapper = objectMapper;
        this.locationPattern = locationPattern;
    }

    @PostConstruct
    void loadCatalogue() throws IOException {
        Resource[] resources = new PathMatchingResourcePatternResolver().getResources(locationPattern);
        for (Resource resource : resources) {
            try (InputStream in = resource.getInputStream()) {
                Restaurant restaurant = objectMapper.readValue(in, Restaurant.class);
                Restaurant previous = byId.putIfAbsent(restaurant.id(), restaurant);
                if (previous != null) {
                    log.warn("Duplicate restaurant id {} in {} — keeping the first one loaded",
                            restaurant.id(), resource.getFilename());
                }
            } catch (IOException e) {
                throw new IOException("Failed to read restaurant file " + resource.getFilename(), e);
            }
        }
        if (byId.isEmpty()) {
            throw new IOException("No restaurant files matched '" + locationPattern + "'");
        }
        log.info("Loaded {} restaurants from '{}'", byId.size(), locationPattern);
    }

    @Override
    public List<Restaurant> findAll() {
        return new ArrayList<>(byId.values());
    }

    @Override
    public Optional<Restaurant> findById(String id) {
        return Optional.ofNullable(byId.get(id));
    }
}
