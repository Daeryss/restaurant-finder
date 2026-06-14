package com.bonial.restaurantfinder.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

class CoordinateTest {

    @Test
    void parsesCanonicalForm() {
        assertThat(Coordinate.parse("x=1,y=2")).isEqualTo(new Coordinate(1, 2));
    }

    @ParameterizedTest
    @CsvSource({
            "'x=1,y=2', 1, 2",
            "'x=10,y=10', 10, 10",
            "' x = 3 , y = 4 ', 3, 4",   // tolerant of surrounding/inner whitespace
            "'x=-2,y=5', -2, 5"          // negative values parse; bounds are enforced elsewhere
    })
    void parsesWithWhitespaceAndSign(String text, int expectedX, int expectedY) {
        assertThat(Coordinate.parse(text)).isEqualTo(new Coordinate(expectedX, expectedY));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "1,2", "x=1", "y=2,x=1", "x=a,y=2", "x=1,y=2,z=3"})
    void rejectsMalformedText(String text) {
        assertThatThrownBy(() -> Coordinate.parse(text))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNull() {
        assertThatThrownBy(() -> Coordinate.parse(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void computesEuclideanDistance() {
        // (1,2) -> (4,4) : sqrt(3^2 + 2^2) = sqrt(13) = 3.6055512...
        assertThat(new Coordinate(1, 2).distanceTo(new Coordinate(4, 4)))
                .isCloseTo(3.6055512, within(1e-6));
    }

    @Test
    void distanceIsSymmetricAndZeroToSelf() {
        Coordinate a = new Coordinate(2, 7);
        Coordinate b = new Coordinate(5, 3);
        assertThat(a.distanceTo(b)).isEqualTo(b.distanceTo(a));
        assertThat(a.distanceTo(a)).isZero();
    }

    @Test
    void rendersAsText() {
        assertThat(new Coordinate(5, 5).asText()).isEqualTo("x=5,y=5");
    }

    @Test
    void parseAndAsTextRoundTrip() {
        assertThat(Coordinate.parse(new Coordinate(8, 8).asText())).isEqualTo(new Coordinate(8, 8));
    }
}
