package com.bonial.restaurantfinder.web;

import com.bonial.restaurantfinder.domain.Coordinate;
import com.bonial.restaurantfinder.domain.Restaurant;
import com.bonial.restaurantfinder.service.RankedRestaurant;
import com.bonial.restaurantfinder.service.RestaurantService;
import com.bonial.restaurantfinder.service.SortOrder;
import com.bonial.restaurantfinder.exception.RestaurantNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RestaurantController.class)
class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RestaurantService restaurantService;

    private static Restaurant restaurant(String id, String name, int n) {
        return new Restaurant(id, name, "Restaurant", "10:00AM-10:00PM",
                "https://tinyurl.com", new Coordinate(n, n));
    }

    @Test
    void searchReturnsUserLocationAndVisibleRestaurants() throws Exception {
        when(restaurantService.findVisibleRestaurants(eq(new Coordinate(1, 2)), any()))
                .thenReturn(List.of(
                        new RankedRestaurant(restaurant("id-1", "Wawa Berlin", 1), 1.0),
                        new RankedRestaurant(restaurant("id-4", "Deseado Steakhaus", 4), Math.sqrt(13))));

        mockMvc.perform(get("/search_locations").param("x", "1").param("y", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['user-location']").value("x=1,y=2"))
                .andExpect(jsonPath("$.locations", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.locations[0].id").value("id-1"))
                .andExpect(jsonPath("$.locations[0].name").value("Wawa Berlin"))
                .andExpect(jsonPath("$.locations[0].coordinate").value("x=1,y=1"))
                .andExpect(jsonPath("$.locations[0].distance").value("1"))
                .andExpect(jsonPath("$.locations[1].distance").value("3.605551"));
    }

    @Test
    void searchPassesRequestedSortOrderToService() throws Exception {
        when(restaurantService.findVisibleRestaurants(any(), eq(SortOrder.DESC))).thenReturn(List.of());

        mockMvc.perform(get("/search_locations").param("x", "1").param("y", "2").param("sort", "desc"))
                .andExpect(status().isOk());
    }

    @Test
    void searchUsesChallengeOrderByDefault() throws Exception {
        when(restaurantService.findVisibleRestaurants(any(), eq(SortOrder.DESC))).thenReturn(List.of());

        mockMvc.perform(get("/search_locations").param("x", "1").param("y", "2"))
                .andExpect(status().isOk());

        verify(restaurantService).findVisibleRestaurants(new Coordinate(1, 2), SortOrder.DESC);
    }

    @Test
    void searchRejectsLocationOutsideCityBoundaries() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "20").param("y", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("outside the city")));
    }

    @Test
    void searchRejectsMissingParameter() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("y")));
    }

    @Test
    void searchRejectsNonIntegerParameter() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "abc").param("y", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchRejectsUnknownSortValue() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "1").param("y", "2").param("sort", "sideways"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("sort")));
    }

    @Test
    void getLocationReturnsRestaurantDetail() throws Exception {
        when(restaurantService.getById("51e1545c-8b65-4d83-82f9-7fcad4a23111"))
                .thenReturn(restaurant("51e1545c-8b65-4d83-82f9-7fcad4a23111", "Da Jia Le", 8));

        mockMvc.perform(get("/location/51e1545c-8b65-4d83-82f9-7fcad4a23111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Da Jia Le"))
                .andExpect(jsonPath("$.type").value("Restaurant"))
                .andExpect(jsonPath("$.id").value("51e1545c-8b65-4d83-82f9-7fcad4a23111"))
                .andExpect(jsonPath("$.openningHours").value("10:00AM-10:00PM"))
                .andExpect(jsonPath("$.coordinate").value("x=8,y=8"));
    }

    @Test
    void getLocationReturns404ForUnknownId() throws Exception {
        when(restaurantService.getById("missing")).thenThrow(new RestaurantNotFoundException("missing"));

        mockMvc.perform(get("/location/missing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("missing")));
    }

    @Test
    void unknownRouteReturns404() throws Exception {
        mockMvc.perform(get("/no-such-route"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void unsupportedMethodReturns405() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/location/51e1545c-8b65-4d83-82f9-7fcad4a23111"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
