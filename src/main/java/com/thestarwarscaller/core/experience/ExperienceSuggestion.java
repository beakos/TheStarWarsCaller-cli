package com.thestarwarscaller.core.experience;

import com.thestarwarscaller.core.Movie;

import java.util.List;
import java.util.Objects;

/** Wrapper used to present curated playlists in the CLI with a friendly title and description. */
public record ExperienceSuggestion(String title, String description, List<Movie> movies) {
    public ExperienceSuggestion {
        Objects.requireNonNull(title, "title");
        Objects.requireNonNull(description, "description");
        movies = sanitize(movies);
    }

    private static List<Movie> sanitize(List<Movie> movies) {
        return movies == null ? List.of() : List.copyOf(movies);
    }
}
