package com.thestarwarscaller.core.story;

import java.util.List;
import java.util.Objects;

/** Immutable bundle of paragraphs covering a movie or series briefing. */
public record StoryBriefing(String title, List<String> paragraphs) {
    public StoryBriefing {
        Objects.requireNonNull(title, "title");
        paragraphs = sanitize(paragraphs);
    }

    private static List<String> sanitize(List<String> parts) {
        return parts == null ? List.of() : List.copyOf(parts);
    }
}
