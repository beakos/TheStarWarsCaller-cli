package com.thestarwarscaller.core.lore;

import java.util.List;
import java.util.Objects;

/** Timeline entry that ties together years, characters, and locations. */
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
        Objects.requireNonNull(era, "era");
        involvedCharacters = sanitize(involvedCharacters);
        locations = sanitize(locations);
        mediaReferences = sanitize(mediaReferences);
    }

    private static List<String> sanitize(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
