package com.bonial.restaurantfinder.domain;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A point on the city grid.
 *
 * <p>The challenge encodes coordinates as the string {@code "x=1,y=2"} both in the restaurant
 * data files and in the API responses, so this type owns the parsing and formatting of that
 * representation. Keeping it here means the rest of the code works with a typed value, not raw
 * strings.
 */
public record Coordinate(int x, int y) {

    private static final Pattern FORMAT = Pattern.compile("\\s*x\\s*=\\s*(-?\\d+)\\s*,\\s*y\\s*=\\s*(-?\\d+)\\s*");

    /**
     * Parses the {@code "x=<int>,y=<int>"} form used by the data files and the API.
     *
     * @throws IllegalArgumentException if the text does not match the expected format
     */
    public static Coordinate parse(String text) {
        if (text == null) {
            throw new IllegalArgumentException("Coordinate text must not be null");
        }
        Matcher matcher = FORMAT.matcher(text);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Invalid coordinate format: '" + text + "', expected 'x=<int>,y=<int>'");
        }
        return new Coordinate(Integer.parseInt(matcher.group(1)), Integer.parseInt(matcher.group(2)));
    }

    /** Euclidean distance between this point and {@code other}. */
    public double distanceTo(Coordinate other) {
        double dx = (double) x - other.x;
        double dy = (double) y - other.y;
        return Math.hypot(dx, dy);
    }

    /** Renders this point in the {@code "x=1,y=2"} form expected by the API responses. */
    public String asText() {
        return "x=" + x + ",y=" + y;
    }
}
