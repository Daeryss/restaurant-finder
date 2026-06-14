package com.bonial.restaurantfinder.repository;

import com.bonial.restaurantfinder.domain.Coordinate;
import com.bonial.restaurantfinder.domain.Restaurant;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonRestaurantRepositoryTest {

    private JsonRestaurantRepository repository;

    @BeforeEach
    void loadFromClasspath() throws Exception {
        repository = new JsonRestaurantRepository(new ObjectMapper(), "classpath:restaurants/*.json");
        repository.loadCatalogue();
    }

    @Test
    void loadsAllRestaurantFiles() {
        assertThat(repository.findAll()).hasSize(10);
    }

    @Test
    void mapsNameKey() {
        Restaurant mantra = repository.findById("19e1545c-8b65-4d83-82f9-7fcad4a23114").orElseThrow();
        assertThat(mantra.name()).isEqualTo("Mantra Restaurant");
        assertThat(mantra.coordinate()).isEqualTo(new Coordinate(2, 2));
    }

    @Test
    void mapsTitleKeyAsName() {
        // The "Goji" file uses "title" instead of "name"; the @JsonAlias must pick it up.
        Restaurant goji = repository.findById("19e1545c-8b65-4d83-82f9-7fcad4a23115").orElseThrow();
        assertThat(goji.name()).isEqualTo("Goji");
        assertThat(goji.coordinate()).isEqualTo(new Coordinate(3, 3));
    }

    @Test
    void mapsOpeningHoursDespiteMisspelledKey() {
        Restaurant daJiaLe = repository.findById("51e1545c-8b65-4d83-82f9-7fcad4a23111").orElseThrow();
        assertThat(daJiaLe.openingHours()).isEqualTo("10:00AM-20:00PM");
    }

    @Test
    void returnsEmptyForUnknownId() {
        assertThat(repository.findById("no-such-id")).isEmpty();
    }

    @Test
    void failsToStartWhenNoRestaurantFilesMatch() {
        JsonRestaurantRepository emptyRepository =
                new JsonRestaurantRepository(new ObjectMapper(), "classpath:restaurants/*.yaml");

        assertThatThrownBy(emptyRepository::loadCatalogue)
                .isInstanceOf(IOException.class)
                .hasMessageContaining("No restaurant files matched");
    }
}
