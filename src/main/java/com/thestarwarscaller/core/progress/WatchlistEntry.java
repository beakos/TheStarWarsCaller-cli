package com.thestarwarscaller.core.progress;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Represents a single entry in the user's holocron watchlist including their current progress and
 * optional rating. Keeping this as a record means it is immutable and trivial to serialise.
 */
public record WatchlistEntry(
        String title,
        MediaType type,
        Status status,
        int rating,
        int timesWatched,
        LocalDate lastWatched) {

    public WatchlistEntry {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        if (rating < 0 || rating > 5) {
            throw new IllegalArgumentException("rating must be between 0 and 5");
        }
        if (timesWatched < 0) {
            throw new IllegalArgumentException("timesWatched cannot be negative");
        }
    }

    /** Identifies whether the entry refers to a movie or a series. */
    public enum MediaType {
        MOVIE,
        SERIES
    }

    /** Tracking state for the entry within the user's backlog. */
    public enum Status {
        PLANNED,
        IN_PROGRESS,
        COMPLETED
    }
}
