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
        this.notableEvents = notableEvents == null ? List.of() : List.copyOf(notableEvents);
        this.featuredIn = featuredIn == null ? List.of() : List.copyOf(featuredIn);
    }
}
