package com.bonial.restaurantfinder.service;

import com.bonial.restaurantfinder.domain.Coordinate;
import com.bonial.restaurantfinder.domain.Restaurant;
import com.bonial.restaurantfinder.exception.RestaurantNotFoundException;
import com.bonial.restaurantfinder.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RestaurantServiceTest {

    /** Restaurants on the x==y diagonal, as in the challenge. Radius B(n,n) = n. */
    private static Restaurant restaurantAt(int n, String name) {
        return new Restaurant(name + "-id", name, "Restaurant", "10:00AM-10:00PM",
                "https://tinyurl.com", new Coordinate(n, n));
    }

    /** In-memory fake — clearer than a mock for exercising the filtering/sorting logic. */
    private static RestaurantRepository repositoryOf(Restaurant... restaurants) {
        List<Restaurant> all = List.of(restaurants);
        return new RestaurantRepository() {
            @Override
            public List<Restaurant> findAll() {
                return all;
            }

            @Override
            public Optional<Restaurant> findById(String id) {
                return all.stream().filter(r -> r.id().equals(id)).findFirst();
            }
        };
    }

    @Test
    void returnsTheRestaurantsFromTheChallengeExample() {
        // Client at (1,2) should see exactly (1,1)..(5,5) per the challenge text.
        RestaurantService service = new RestaurantService(repositoryOf(
                restaurantAt(1, "Wawa"), restaurantAt(2, "Mantra"), restaurantAt(3, "Goji"),
                restaurantAt(4, "Deseado"), restaurantAt(5, "FireTiger"),
                restaurantAt(6, "Ottenthal"), restaurantAt(7, "Marjellchen")));

        List<RankedRestaurant> visible =
                service.findVisibleRestaurants(new Coordinate(1, 2), SortOrder.ASC);

        assertThat(visible).extracting(r -> r.restaurant().coordinate().x())
                .containsExactly(1, 2, 3, 4, 5); // ascending by distance; (6,6) and (7,7) out of view
    }

    @Test
    void excludesRestaurantsOnTheCircleBoundaryWhenStrictlyOutside() {
        // (6,6) from (1,2): distance sqrt(41) ~= 6.403 > radius 6 -> not visible.
        RestaurantService service = new RestaurantService(repositoryOf(restaurantAt(6, "Ottenthal")));
        assertThat(service.findVisibleRestaurants(new Coordinate(1, 2), SortOrder.ASC)).isEmpty();
    }

    @Test
    void includesRestaurantExactlyOnTheBoundary() {
        // (5,5) from (1,2): distance is exactly 5 == radius 5 -> visible (boundary inclusive).
        RestaurantService service = new RestaurantService(repositoryOf(restaurantAt(5, "FireTiger")));
        assertThat(service.findVisibleRestaurants(new Coordinate(1, 2), SortOrder.ASC)).hasSize(1);
    }

    @Test
    void sortsAscendingByDistanceByDefault() {
        RestaurantService service = new RestaurantService(repositoryOf(
                restaurantAt(5, "FireTiger"), restaurantAt(3, "Goji"), restaurantAt(4, "Deseado")));

        List<RankedRestaurant> visible =
                service.findVisibleRestaurants(new Coordinate(1, 2), SortOrder.ASC);

        assertThat(visible).extracting(RankedRestaurant::distance).isSorted();
    }

    @Test
    void sortsDescendingWhenRequested() {
        RestaurantService service = new RestaurantService(repositoryOf(
                restaurantAt(5, "FireTiger"), restaurantAt(3, "Goji"), restaurantAt(4, "Deseado")));

        List<RankedRestaurant> visible =
                service.findVisibleRestaurants(new Coordinate(1, 2), SortOrder.DESC);

        assertThat(visible).extracting(RankedRestaurant::distance)
                .isSortedAccordingTo((a, b) -> Double.compare(b, a));
    }

    @Test
    void usesCoordinateAsStableTieBreaker() {
        RestaurantService service = new RestaurantService(repositoryOf(
                restaurantAt(1, "Wawa"), restaurantAt(2, "Mantra")));

        assertThat(service.findVisibleRestaurants(new Coordinate(1, 2), SortOrder.ASC))
                .extracting(r -> r.restaurant().name())
                .containsExactly("Wawa", "Mantra");
        assertThat(service.findVisibleRestaurants(new Coordinate(1, 2), SortOrder.DESC))
                .extracting(r -> r.restaurant().name())
                .containsExactly("Mantra", "Wawa");
    }

    @Test
    void getByIdReturnsTheRestaurant() {
        RestaurantService service = new RestaurantService(repositoryOf(restaurantAt(8, "DaJiaLe")));
        assertThat(service.getById("DaJiaLe-id").name()).isEqualTo("DaJiaLe");
    }

    @Test
    void getByIdThrowsWhenMissing() {
        RestaurantService service = new RestaurantService(repositoryOf());
        assertThatThrownBy(() -> service.getById("does-not-exist"))
                .isInstanceOf(RestaurantNotFoundException.class)
                .hasMessageContaining("does-not-exist");
    }
}
