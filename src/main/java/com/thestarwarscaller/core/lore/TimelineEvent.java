package com.thestarwarscaller.core.lore;

import java.util.List;
import java.util.Objects;

/** Represents a canon event we can surface in timeline queries. */
public record TimelineEvent(
        String title,
        String era,
        int year,
        String description,
        List<String> involvedCharacters,
        List<String> locations,
        List<String> mediaReferences) {

    public TimelineEvent {
        Objects.requireNonNull(title, "title");
        this.involvedCharacters = involvedCharacters == null ? List.of() : List.copyOf(involvedCharacters);
        this.locations = locations == null ? List.of() : List.copyOf(locations);
        this.mediaReferences = mediaReferences == null ? List.of() : List.copyOf(mediaReferences);
    }
}
