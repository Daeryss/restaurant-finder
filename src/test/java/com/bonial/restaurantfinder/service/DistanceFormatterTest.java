package com.bonial.restaurantfinder.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class DistanceFormatterTest {

    @ParameterizedTest
    @CsvSource({
            "5.0, 5",                 // whole number -> no decimals
            "1.0, 1",
            "0.0, 0",
            "2.2360679, 2.236068",    // sqrt(5),  rounded to 6 places
            "3.6055512, 3.605551",    // sqrt(13), rounded to 6 places
            "1.4142135, 1.414214"     // sqrt(2),  rounded to 6 places
    })
    void formatsToSixPlacesTrimmingTrailingZeros(double input, String expected) {
        assertThat(DistanceFormatter.format(input)).isEqualTo(expected);
    }

    @Test
    void matchesTheChallengeSampleValues() {
        assertThat(DistanceFormatter.format(5.0)).isEqualTo("5");
        assertThat(DistanceFormatter.format(Math.sqrt(13))).isEqualTo("3.605551");
        assertThat(DistanceFormatter.format(Math.sqrt(5))).isEqualTo("2.236068");
        assertThat(DistanceFormatter.format(1.0)).isEqualTo("1");
    }
}
