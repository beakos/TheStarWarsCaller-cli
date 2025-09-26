package com.thestarwarscaller.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Keeps track of what the user likes and remembers between CLI sessions.
 * Picture this as your personal datapad storing favourite holovids and search history.
 */
public final class UserPreferences {
    private static final int MAX_RECENT_SEARCHES = 10;

    private final Set<String> favoriteMovies;
    private final Set<String> favoriteSeries;
    private final Set<String> acknowledgedAchievements;
    private boolean colorizedOutput;
    private boolean emojiOutput;
    private String lastMovieFilter;
    private String lastSeriesFilter;
    private final Deque<String> recentSearches;

    public UserPreferences() {
        this.favoriteMovies = new LinkedHashSet<>();
        this.favoriteSeries = new LinkedHashSet<>();
        this.acknowledgedAchievements = new LinkedHashSet<>();
        this.recentSearches = new ArrayDeque<>();
    }

    /** @return read-only view of favourite movie titles. */
    public Set<String> getFavoriteMovies() {
        return Collections.unmodifiableSet(favoriteMovies);
    }

    /** @return read-only view of favourite series titles. */
    public Set<String> getFavoriteSeries() {
        return Collections.unmodifiableSet(favoriteSeries);
    }

    /** @return the last filter applied to movies. */
    public String getLastMovieFilter() {
        return lastMovieFilter;
    }

    public void setLastMovieFilter(String lastMovieFilter) {
        this.lastMovieFilter = lastMovieFilter;
    }

    /** @return the last filter applied to series. */
    public String getLastSeriesFilter() {
        return lastSeriesFilter;
    }

    public void setLastSeriesFilter(String lastSeriesFilter) {
        this.lastSeriesFilter = lastSeriesFilter;
    }

    /** @return copy of the recent search terms (most recent first). */
    public List<String> getRecentSearches() {
        return List.copyOf(recentSearches);
    }

    /** @return {@code true} when the user has enabled ANSI colour output. */
    public boolean isColorizedOutput() {
        return colorizedOutput;
    }

    public void setColorizedOutput(boolean colorizedOutput) {
        this.colorizedOutput = colorizedOutput;
    }

    /** @return {@code true} when the user wants emoji embellishments in listings. */
    public boolean isEmojiOutput() {
        return emojiOutput;
    }

    public void setEmojiOutput(boolean emojiOutput) {
        this.emojiOutput = emojiOutput;
    }

    /** @return achievements the user has already celebrated (so we do not spam them twice). */
    public Set<String> getAcknowledgedAchievements() {
        return Collections.unmodifiableSet(acknowledgedAchievements);
    }

    /** Records an achievement as acknowledged. */
    public void acknowledgeAchievement(String achievementId) {
        if (achievementId != null) {
            acknowledgedAchievements.add(achievementId);
        }
    }

    /** Resets the acknowledgement list; handy for debug commands. */
    public void clearAcknowledgedAchievements() {
        acknowledgedAchievements.clear();
    }

    /** Adds a favourite movie by title. */
    public void addFavoriteMovie(String title) {
        favoriteMovies.add(Objects.requireNonNull(title));
    }

    /** Removes the given movie from favourites. */
    public void removeFavoriteMovie(String title) {
        favoriteMovies.remove(title);
    }

    /** Adds a favourite series by title. */
    public void addFavoriteSeries(String title) {
        favoriteSeries.add(Objects.requireNonNull(title));
    }

    /** Removes the given series from favourites. */
    public void removeFavoriteSeries(String title) {
        favoriteSeries.remove(title);
    }

    /** Stores a search term in the history (newest at the front). */
    public void addRecentSearch(String term) {
        if (term == null || term.isBlank()) {
            return;
        }
        recentSearches.remove(term);
        recentSearches.addFirst(term);
        while (recentSearches.size() > MAX_RECENT_SEARCHES) {
            recentSearches.removeLast();
        }
    }

    /**
     * Copies data from another preference instance.
     * Handy when loading from disk and reusing an existing object reference.
     */
    public void hydrateFrom(UserPreferences other) {
        favoriteMovies.clear();
        favoriteMovies.addAll(other.favoriteMovies);
        favoriteSeries.clear();
        favoriteSeries.addAll(other.favoriteSeries);
        acknowledgedAchievements.clear();
        acknowledgedAchievements.addAll(other.acknowledgedAchievements);
        colorizedOutput = other.colorizedOutput;
        emojiOutput = other.emojiOutput;
        lastMovieFilter = other.lastMovieFilter;
        lastSeriesFilter = other.lastSeriesFilter;
        recentSearches.clear();
        recentSearches.addAll(other.recentSearches);
    }

    /** Snapshot used when writing preferences back to JSON. */
    public PrefSnapshot snapshot() {
        return new PrefSnapshot(
                new ArrayList<>(favoriteMovies),
                new ArrayList<>(favoriteSeries),
                lastMovieFilter,
                lastSeriesFilter,
                new ArrayList<>(recentSearches),
                colorizedOutput,
                emojiOutput,
                new ArrayList<>(acknowledgedAchievements));
    }

    /**
     * Simple record to carry preference data around.
     * Bonus trivia: this now also stores presentation toggles and achievement state.
     */
    public record PrefSnapshot(List<String> favoriteMovies,
                               List<String> favoriteSeries,
                               String lastMovieFilter,
                               String lastSeriesFilter,
                               List<String> recentSearches,
                               boolean colorizedOutput,
                               boolean emojiOutput,
                               List<String> acknowledgedAchievements) {
    }
}
