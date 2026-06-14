package com.bonial.restaurantfinder.web;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end tests over the full Spring context and the real JSON catalogue, anchored on the
 * canonical example from the challenge: a client at (1,2) sees restaurants (1,1)..(5,5).
 */
@SpringBootTest
@AutoConfigureMockMvc
class RestaurantApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void searchFromTheChallengeExampleReturnsTheFiveVisibleRestaurants() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "1").param("y", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['user-location']").value("x=1,y=2"))
                .andExpect(jsonPath("$.locations", hasSize(5)))
                .andExpect(jsonPath("$.locations[*].name", containsInAnyOrder(
                        "Wawa Berlin", "Mantra Restaurant", "Goji", "Deseado Steakhaus", "Fire Tiger")))
                // default ordering reproduces the challenge response: farthest first
                .andExpect(jsonPath("$.locations[0].name").value("Fire Tiger"))
                .andExpect(jsonPath("$.locations[0].distance").value("5"));
    }

    @Test
    void searchSortsByDistanceAscendingWhenRequested() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "1").param("y", "2").param("sort", "asc"))
                .andExpect(status().isOk())
                // distances rendered as strings, but the underlying order is non-decreasing
                .andExpect(jsonPath("$.locations[*].distance",
                        contains("1", "1", "2.236068", "3.605551", "5")));
    }

    @Test
    void searchSortsByDistanceDescendingWhenRequested() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "1").param("y", "2").param("sort", "desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations[0].name").value("Fire Tiger"))
                .andExpect(jsonPath("$.locations[*].distance",
                        contains("5", "3.605551", "2.236068", "1", "1")))
                .andExpect(jsonPath("$.locations[3].name").value("Mantra Restaurant"))
                .andExpect(jsonPath("$.locations[4].name").value("Wawa Berlin"));
    }

    @Test
    void searchFromCitySquareSeesNoRestaurant() throws Exception {
        // From the origin the distance to (n,n) is n*sqrt(2) ~= 1.414n, always greater than the
        // radius n, so no restaurant's bounding circle reaches the city square. Empty list, not an error.
        mockMvc.perform(get("/search_locations").param("x", "0").param("y", "0"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['user-location']").value("x=0,y=0"))
                .andExpect(jsonPath("$.locations", hasSize(0)));
    }

    @Test
    void getLocationReturnsFullDetailForKnownId() throws Exception {
        mockMvc.perform(get("/location/51e1545c-8b65-4d83-82f9-7fcad4a23111"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Da Jia Le"))
                .andExpect(jsonPath("$.type").value("Restaurant"))
                .andExpect(jsonPath("$.id").value("51e1545c-8b65-4d83-82f9-7fcad4a23111"))
                .andExpect(jsonPath("$.openningHours").value("10:00AM-20:00PM"))
                .andExpect(jsonPath("$.image").value("https://tinyurl.com"))
                .andExpect(jsonPath("$.coordinate").value("x=8,y=8"));
    }

    @Test
    void getLocationResolvesRestaurantStoredWithTitleKey() throws Exception {
        // The "Goji" file uses "title" rather than "name" — it must still resolve end-to-end.
        mockMvc.perform(get("/location/19e1545c-8b65-4d83-82f9-7fcad4a23115"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Goji"))
                .andExpect(jsonPath("$.coordinate").value("x=3,y=3"));
    }

    @Test
    void getLocationReturns404ForUnknownId() throws Exception {
        mockMvc.perform(get("/location/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void listLocationsReturnsTheWholeCatalogueSortedByX() throws Exception {
        mockMvc.perform(get("/locations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(10)))
                .andExpect(jsonPath("$[0].coordinate").value("x=1,y=1"))   // sorted by x ascending
                .andExpect(jsonPath("$[9].coordinate").value("x=10,y=10"))
                .andExpect(jsonPath("$[*].name", hasItem("Goji")));         // title-keyed entry resolves
    }

    @Test
    void searchAtTheFarCornerOfTheCityIsValid() throws Exception {
        // (14,10) is the inclusive max corner (max x = 14, max y = 10): must be accepted, not an
        // off-by-one rejection.
        mockMvc.perform(get("/search_locations").param("x", "14").param("y", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['user-location']").value("x=14,y=10"));
    }

    @Test
    void searchRejectsLocationPastTheXBoundary() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "15").param("y", "2"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchRejectsLocationPastTheYBoundary() throws Exception {
        // y is bounded at 10, independently of x's bound of 14 — guards against using one max for both.
        mockMvc.perform(get("/search_locations").param("x", "5").param("y", "11"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchRejectsNegativeCoordinate() throws Exception {
        mockMvc.perform(get("/search_locations").param("x", "-1").param("y", "5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void searchFromOnTopOfARestaurantReportsZeroDistance() throws Exception {
        // A client standing exactly on Fire Tiger (5,5) sees it at distance 0, formatted as "0".
        mockMvc.perform(get("/search_locations").param("x", "5").param("y", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.locations[?(@.name == 'Fire Tiger')].distance").value("0"))
                // Mantra (2,2) is too far from (5,5) for its radius-2 circle to reach: must be absent.
                .andExpect(jsonPath("$.locations[?(@.name == 'Mantra Restaurant')]").isEmpty());
    }
}
