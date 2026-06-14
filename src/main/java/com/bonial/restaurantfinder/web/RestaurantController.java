package com.bonial.restaurantfinder.web;

import com.bonial.restaurantfinder.config.CityProperties;
import com.bonial.restaurantfinder.domain.Coordinate;
import com.bonial.restaurantfinder.service.RankedRestaurant;
import com.bonial.restaurantfinder.service.RestaurantService;
import com.bonial.restaurantfinder.service.SortOrder;
import com.bonial.restaurantfinder.exception.BadRequestException;
import com.bonial.restaurantfinder.web.dto.RestaurantDetail;
import com.bonial.restaurantfinder.web.dto.SearchResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;

/**
 * HTTP entry points for the two challenge endpoints:
 * <ul>
 *   <li>{@code GET /search_locations?x=&y=&sort=} — restaurants visible from the client location</li>
 *   <li>{@code GET /location/{id}} — full detail of one restaurant</li>
 * </ul>
 *
 * <p>The controller is intentionally thin: it validates input, delegates to {@link RestaurantService},
 * and maps the result to a DTO. Business rules live in the service; error-to-HTTP mapping lives in the
 * {@code @RestControllerAdvice}.
 */
@RestController
public class RestaurantController {

    private final RestaurantService restaurantService;
    private final CityProperties city;

    public RestaurantController(RestaurantService restaurantService, CityProperties city) {
        this.restaurantService = restaurantService;
        this.city = city;
    }

    @GetMapping("/search_locations")
    public SearchResponse searchLocations(
            @RequestParam int x,
            @RequestParam int y,
            @RequestParam(defaultValue = "desc") String sort) {

        if (!city.contains(x, y)) {
            throw new BadRequestException(
                    "Location x=%d,y=%d is outside the city boundaries (0..%d, 0..%d)"
                            .formatted(x, y, city.maxX(), city.maxY()));
        }

        Coordinate clientLocation = new Coordinate(x, y);
        List<RankedRestaurant> visible = restaurantService.findVisibleRestaurants(clientLocation, parseSort(sort));
        return SearchResponse.of(clientLocation, visible);
    }

    @GetMapping("/location/{id}")
    public RestaurantDetail getLocation(@PathVariable String id) {
        return RestaurantDetail.from(restaurantService.getById(id));
    }

    private static SortOrder parseSort(String sort) {
        try {
            return SortOrder.valueOf(sort.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid sort value '" + sort + "', expected 'asc' or 'desc'");
        }
    }
}
