package com.thestarwarscaller.core.lore;

import java.util.List;
import java.util.Objects;

/** Captures high-level data about a planet so we can cross-reference events and appearances. */
public record PlanetProfile(
        String name,
        String region,
        String description,
        List<String> notableEvents,
        List<String> featuredIn) {

    public PlanetProfile {
        Objects.requireNonNull(name, "name");
        notableEvents = sanitize(notableEvents);
        featuredIn = sanitize(featuredIn);
    }

    private static List<String> sanitize(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
