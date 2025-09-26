package com.thestarwarscaller.core.story;

import java.util.List;
import java.util.Objects;

/** Structured payload describing a narrative briefing for CLI "story mode". */
public record StoryBriefing(String title, List<String> paragraphs) {
    public StoryBriefing {
        Objects.requireNonNull(title, "title");
        this.paragraphs = paragraphs == null ? List.of() : List.copyOf(paragraphs);
    }
}
