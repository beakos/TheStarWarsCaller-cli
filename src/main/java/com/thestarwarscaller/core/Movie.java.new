package com.thestarwarscaller.core;

import com.thestarwarscaller.core.view.MoviePresentationOptions;
import com.thestarwarscaller.core.view.MovieView;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents a single Star Wars film in memory.
 * Picture this class as a holocron that stores every fact a fan might need.
 */
public final class Movie {
    // The full marketing title, such as "Star Wars: Episode IV - A New Hope".
    private final String title;
    // Optional numeric episode number for saga entries. Null when it is not part of the saga.
    private final Integer episodeNumber;
    // Optional Roman numeral (because everything looks cooler in Aurebesh).
    private final String episodeRoman;
    // The year the galaxy first saw this film.
    private final int releaseYear;
    // Which high-level bucket the film belongs to (Saga, Anthology, etc.).
    private final MovieCategory category;
    // Timeline era label so fans can follow the story in chronological order.
    private final String era;
    // Short blurb of what goes down in that film.
    private final String synopsis;
    // Where viewers can currently stream the adventure.
    private final String streaming;
    // Extra notes for trivia lovers (directors, fun facts, and womp rat-sized details).
    private final String notes;

    /**
     * Builds a new immutable movie record.
     * Think of the constructor as loading a data card into the Jedi Archives.
     */
    public Movie(
            String title,
            Integer episodeNumber,
            String episodeRoman,
            int releaseYear,
            MovieCategory category,
            String era,
            String synopsis,
            String streaming,
            String notes) {
        // Title is mandatory because every record should have a friendly name.
        this.title = Objects.requireNonNull(title, "title");
        // Episode information is optional so we simply store whatever we receive.
        this.episodeNumber = episodeNumber;
        this.episodeRoman = episodeRoman;
        // Release year is kept as a primitive for quick sorting.
        this.releaseYear = releaseYear;
        // Category is compulsory; it powers many menu groupings.
        this.category = Objects.requireNonNull(category, "category");
        this.era = era;
        this.synopsis = synopsis;
        this.streaming = streaming;
        this.notes = notes;
    }

    /** @return the official movie title; handy for menus and posters. */
    public String getTitle() {
        return title;
    }

    /** @return the numeric Skywalker Saga episode number, or {@code null} if not applicable. */
    public Integer getEpisodeNumber() {
        return episodeNumber;
    }

    /** @return the fancier Roman numeral version of the episode number. */
    public String getEpisodeRoman() {
        return episodeRoman;
    }

    /** @return the release year so we can create watch orders and trivia nights. */
    public int getReleaseYear() {
        return releaseYear;
    }

    /** @return the category bucket used for grouping movies. */
    public MovieCategory getCategory() {
        return category;
    }

    /** @return the canonical timeline era (e.g., "Galactic Civil War"). */
    public String getEra() {
        return era;
    }

    /** @return the quick synopsis for fans who need a refresher. */
    public String getSynopsis() {
        return synopsis;
    }

    /** @return the streaming service or platform reference. */
    public String getStreaming() {
        return streaming;
    }

    /** @return bonus notes that might impress a holodrama quiz master. */
    public String getNotes() {
        return notes;
    }

    /**
     * Checks whether this movie matches a case-insensitive query.
     * This lets padawans find entries even if they forget exact capitals.
     */
    public boolean matches(String query) {
        // Bail out quickly if the caller passed a null query.
        if (query == null) {
            return false;
        }
        // Remove leading/trailing whitespace so inputs like "  empire " still work.
        String trimmed = query.trim();
        if (trimmed.isEmpty()) {
            return false;
        }
        // Normalise to lower-case once so the checks below can reuse the same value.
        String lower = trimmed.toLowerCase(Locale.ROOT);
        // Title is the most common place to find a match.
        if (title.toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        // Check the era so timeline-focused searches succeed.
        if (era != null && era.toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        // The synopsis often holds memorable keywords, so look there next.
        if (synopsis != null && synopsis.toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        // Finally, peek at trivia notes for cast names, locations, or production tidbits.
        return notes != null && notes.toLowerCase(Locale.ROOT).contains(lower);
    }

    /**
     * Creates a multi-line description ready for CLI output using default presentation options.
     * Fun fact: you can read the result aloud like an opening crawl.
     */
    public String describe() {
        // Delegate to the richer overload with default styling toggles.
        return describe(MoviePresentationOptions.defaults());
    }

    /**
     * Creates a multi-line description with optional colour and emoji embellishments.
     * The options object is where we toggle ANSI colours or holo-emojis on and off.
     */
    public String describe(MoviePresentationOptions options) {
        // Convert this movie into a MovieView and ask it to render the final text.
        return toView(options).render();
    }

    /**
     * Generates a {@link MovieView} DTO that callers can use for testing or custom rendering.
     * Keeping formatting details in one place stops the CLI class from ballooning.
     */
    public MovieView toView(MoviePresentationOptions options) {
        // Guard against null so consumers can pass null to opt into defaults.
        MoviePresentationOptions safeOptions = options != null ? options : MoviePresentationOptions.defaults();
        // The view performs the heavy formatting work up front.
        return new MovieView(this, safeOptions);
    }
}
