package com.bonial.restaurantfinder.domain;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

import java.io.IOException;

/**
 * Reads the {@code "x=1,y=2"} string form of a coordinate (as used in the JSON data files) into a
 * typed {@link Coordinate}. Lives next to the model it deserialises so the wire-format knowledge for
 * a coordinate stays in one place.
 */
public class CoordinateDeserializer extends JsonDeserializer<Coordinate> {

    @Override
    public Coordinate deserialize(JsonParser parser, DeserializationContext context) throws IOException {
        return Coordinate.parse(parser.getValueAsString());
    }
}
