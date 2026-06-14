package com.bonial.restaurantfinder.service;

/** Direction in which search results are ordered by distance from the client. */
public enum SortOrder {
    /** Closest restaurants first. */
    ASC,
    /** Farthest restaurants first — the ordering shown in the challenge response. */
    DESC
}
