package com.thestarwarscaller.core.progress;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Snapshot of a single viewing session used for the history log. Keeping events lightweight makes
 * it cheap to append after every completed watch without slowing down the CLI.
 */
public record WatchEvent(
        String title,
        WatchlistEntry.MediaType type,
        LocalDateTime watchedAt,
        int rating,
        String notes) {

    public WatchEvent {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(watchedAt, "watchedAt");
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 0 and 5");
        }
    }
}
