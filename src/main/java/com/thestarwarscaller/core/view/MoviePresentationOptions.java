package com.thestarwarscaller.core.view;

import com.thestarwarscaller.core.MovieCategory;

/**
 * Encapsulates rendering options for movie descriptions so the CLI can toggle flair without
 * touching low-level formatting logic. Builders let the caller opt into ANSI colours or emojis
 * while keeping defaults conservative for plain terminals.
 */
public final class MoviePresentationOptions {
    /** Shared default instance used whenever callers do not specify custom settings. */
    private static final MoviePresentationOptions DEFAULT = MoviePresentationOptions.builder().build();

    private final boolean colorize;
    private final boolean includeEmoji;
    private final boolean includeSynopsis;
    private final boolean includeNotes;

    private MoviePresentationOptions(Builder builder) {
        this.colorize = builder.colorize;
        this.includeEmoji = builder.includeEmoji;
        this.includeSynopsis = builder.includeSynopsis;
        this.includeNotes = builder.includeNotes;
    }

    /** @return an options instance with sensible CLI-safe defaults. */
    public static MoviePresentationOptions defaults() {
        return DEFAULT;
    }

    /** @return fluent builder so callers can tweak individual flags. */
    public static Builder builder() {
        return new Builder();
    }

    /** @return whether ANSI colours should decorate text output. */
    public boolean colorize() {
        return colorize;
    }

    /** @return whether emoji markers should accompany marquee text. */
    public boolean includeEmoji() {
        return includeEmoji;
    }

    /** @return flag indicating if synopsis lines should be appended. */
    public boolean includeSynopsis() {
        return includeSynopsis;
    }

    /** @return flag indicating if trivia notes should be appended. */
    public boolean includeNotes() {
        return includeNotes;
    }

    /**
     * Applies colour to the supplied headline based on the movie category while respecting the
     * caller's colour preference. When colour is disabled the text is returned unchanged.
     */
    public String decorateHeadline(String headline, MovieCategory category) {
        if (!colorize) {
            return headline;
        }
        return AnsiPalette.forCategory(category) + headline + AnsiPalette.RESET;
    }

    /**
     * Wraps badge text (era, watch status, etc.) in a consistent highlight colour.
     * Disables clean output when colour is not desired.
     */
    public String decorateBadge(String badge) {
        if (!colorize) {
            return badge;
        }
        return AnsiPalette.BADGE + badge + AnsiPalette.RESET;
    }

    /**
     * Provides an emoji representing the movie category when the flag is enabled.
     * Returns an empty string otherwise so callers can concatenate safely.
     */
    public String emojiForCategory(MovieCategory category) {
        if (!includeEmoji) {
            return "";
        }
        return switch (category) {
            case SAGA -> "\uD83D\uDE80"; // Rocket emoji
            case ANTHOLOGY -> "\uD83C\uDF7F"; // Popcorn emoji
            case ANIMATED_FEATURE -> "\uD83D\uDC7B"; // Ghost emoji
            case TV_FILM -> "\uD83D\uDCFA"; // Television emoji
            case LEGO_SPECIAL -> "\uD83E\uDDF3"; // Brick emoji
        };
    }

    /** Fluent builder with opt-in flags for CLI customisation. */
    public static final class Builder {
        private boolean colorize;
        private boolean includeEmoji;
        private boolean includeSynopsis = true;
        private boolean includeNotes = true;

        Builder() {
            // package-private to encourage use of MoviePresentationOptions.builder().
        }

        /** Enables ANSI colouring for the rendered text. */
        public Builder withColor() {
            this.colorize = true;
            return this;
        }

        /** Enables emoji output to spice up headings. */
        public Builder withEmoji() {
            this.includeEmoji = true;
            return this;
        }

        /** Omits synopsis output for compact listings. */
        public Builder withoutSynopsis() {
            this.includeSynopsis = false;
            return this;
        }

        /** Omits notes output when trivia should stay hidden. */
        public Builder withoutNotes() {
            this.includeNotes = false;
            return this;
        }

        /** Builds the immutable options object. */
        public MoviePresentationOptions build() {
            return new MoviePresentationOptions(this);
        }
    }

    /** Simple ANSI palette keeping escape sequences in one place. */
    private static final class AnsiPalette {
        private static final String RESET = "\u001B[0m";
        private static final String BADGE = "\u001B[38;5;33m";

        private static String forCategory(MovieCategory category) {
            return switch (category) {
                case SAGA -> "\u001B[38;5;214m";
                case ANTHOLOGY -> "\u001B[38;5;39m";
                case ANIMATED_FEATURE -> "\u001B[38;5;170m";
                case TV_FILM -> "\u001B[38;5;118m";
                case LEGO_SPECIAL -> "\u001B[38;5;220m";
            };
        }
    }
}
