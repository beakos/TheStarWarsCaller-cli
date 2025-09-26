package com.thestarwarscaller.core.view;

import com.thestarwarscaller.core.Movie;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * Lightweight DTO that pre-computes the strings the CLI needs when it renders a {@link Movie}.
 * The extra object stops presentation rules from leaking into command handlers and makes unit
 * testing easier because we can assert against small immutable strings instead of entire movies.
 */
public final class MovieView {
    private final Movie movie;
    private final MoviePresentationOptions options;
    private final String headline;
    private final List<String> badges;
    private final String badgeLine;
    private final String synopsis;
    private final String notes;
    private final int triviaCount;
    private final String emoji;

    /**
     * Builds a view object from a movie and rendering options. Every expensive string operation is
     * executed eagerly in the constructor so repeated calls to {@link #render()} stay cheap.
     */
    public MovieView(Movie movie, MoviePresentationOptions options) {
        this.movie = Objects.requireNonNull(movie, "movie");
        this.options = Objects.requireNonNull(options, "options");
        this.synopsis = movie.getSynopsis();
        this.notes = movie.getNotes();
        this.emoji = options.emojiForCategory(movie.getCategory());
        this.triviaCount = calculateTriviaCount(notes);
        this.badges = buildBadges();
        this.badgeLine = buildBadgeLine();
        this.headline = buildHeadline();
    }

    /** @return immutable list of the raw badges calculated for the movie. */
    public List<String> badges() {
        return badges;
    }

    /** @return preformatted heading including optional ANSI colour and emoji flair. */
    public String headline() {
        return headline;
    }

    /** @return how many individual trivia morsels we discovered in the notes field. */
    public int triviaCount() {
        return triviaCount;
    }

    /**
     * @return the fully rendered multi-line description honouring the supplied options.
     * The format mirrors {@link Movie#describe()} so existing callers can swap in this type.
     */
    public String render() {
        StringBuilder sb = new StringBuilder();
        sb.append(headline);
        if (!badgeLine.isEmpty()) {
            sb.append('\n').append("    ").append(badgeLine);
        }
        if (options.includeSynopsis() && synopsis != null && !synopsis.isBlank()) {
            sb.append('\n').append("    ").append(synopsis);
        }
        if (options.includeNotes() && notes != null && !notes.isBlank()) {
            sb.append('\n').append("    Notes: ").append(notes);
        }
        return sb.toString();
    }

    /** Builds the headline string once, weaving in episode labels and emoji if requested. */
    private String buildHeadline() {
        StringBuilder sb = new StringBuilder();
        if (!emoji.isEmpty()) {
            sb.append(emoji).append(' ');
        }
        if (movie.getEpisodeNumber() != null) {
            sb.append("Episode ");
            sb.append(movie.getEpisodeRoman() != null ? movie.getEpisodeRoman() : movie.getEpisodeNumber());
            sb.append(" - ");
        }
        sb.append(movie.getTitle());
        sb.append(" (").append(movie.getReleaseYear()).append(")");
        return options.decorateHeadline(sb.toString(), movie.getCategory());
    }

    /** Collects the badges we display underneath the headline for quick-glance metadata. */
    private List<String> buildBadges() {
        List<String> computed = new ArrayList<>();
        computed.add("Category: " + movie.getCategory().getDisplayName());
        if (movie.getEra() != null && !movie.getEra().isBlank()) {
            computed.add("Era: " + movie.getEra());
        }
        computed.add("Release: " + movie.getReleaseYear());
        if (movie.getStreaming() != null && !movie.getStreaming().isBlank()) {
            computed.add("Watch: " + movie.getStreaming());
        }
        if (triviaCount > 0) {
            computed.add("Trivia x" + triviaCount);
        }
        return Collections.unmodifiableList(computed);
    }

    /** Joins badges into a readable line, applying colour when enabled. */
    private String buildBadgeLine() {
        if (badges.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(" | ");
        for (String badge : badges) {
            joiner.add(options.decorateBadge(badge));
        }
        return joiner.toString();
    }

    /** Counts how many trivia fragments exist by splitting notes on semicolons or line breaks. */
    private static int calculateTriviaCount(String rawNotes) {
        if (rawNotes == null || rawNotes.isBlank()) {
            return 0;
        }
        int count = 0;
        for (String fragment : rawNotes.split("[;\\n]+")) {
            if (!fragment.isBlank()) {
                count++;
            }
        }
        return count;
    }
}
