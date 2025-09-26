package com.thestarwarscaller.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles reading and writing {@link UserPreferences} to disk using JSON.
 * Imagine it as the datapad sync feature that makes sure favourites survive hyperspace jumps.
 */
public final class PreferencesManager {
    private PreferencesManager() {
    }

    /** Loads user preferences from the given file, falling back to empty defaults. */
    public static UserPreferences load(Path file) throws IOException {
        if (!Files.exists(file)) {
            return new UserPreferences();
        }
        String json = Files.readString(file);
        Object parsed = JsonParser.parse(json);
        UserPreferences preferences = new UserPreferences();
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) map;
            restore(preferences, cast);
        } else {
            throw new IllegalArgumentException("Preferences file must be a JSON object");
        }
        return preferences;
    }

    /** Saves the current preferences to disk as JSON. */
    public static void save(Path file, UserPreferences preferences) throws IOException {
        UserPreferences.PrefSnapshot snapshot = preferences.snapshot();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("favoriteMovies", new ArrayList<>(snapshot.favoriteMovies()));
        map.put("favoriteSeries", new ArrayList<>(snapshot.favoriteSeries()));
        map.put("lastMovieFilter", snapshot.lastMovieFilter());
        map.put("lastSeriesFilter", snapshot.lastSeriesFilter());
        map.put("recentSearches", new ArrayList<>(snapshot.recentSearches()));
        map.put("colorizedOutput", snapshot.colorizedOutput());
        map.put("emojiOutput", snapshot.emojiOutput());
        map.put("acknowledgedAchievements", new ArrayList<>(snapshot.acknowledgedAchievements()));
        String json = JsonWriter.write(map);
        Files.createDirectories(file.getParent() == null ? Path.of(".") : file.getParent());
        Files.writeString(file, json);
    }

    /**
     * Restores data from a parsed map into the provided preferences instance.
     * Uses gentle null checks so corrupted files do not crash the entire program.
     */
    private static void restore(UserPreferences preferences, Map<String, Object> map) {
        Object favMovies = map.get("favoriteMovies");
        if (favMovies instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof String title) {
                    preferences.addFavoriteMovie(title);
                }
            }
        }
        Object favSeries = map.get("favoriteSeries");
        if (favSeries instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof String title) {
                    preferences.addFavoriteSeries(title);
                }
            }
        }
        Object lastMovie = map.get("lastMovieFilter");
        if (lastMovie instanceof String lm) {
            preferences.setLastMovieFilter(lm);
        }
        Object lastSeries = map.get("lastSeriesFilter");
        if (lastSeries instanceof String ls) {
            preferences.setLastSeriesFilter(ls);
        }
        Object searches = map.get("recentSearches");
        if (searches instanceof List<?> list) {
            List<?> reversed = new ArrayList<>(list);
            for (Object entry : reversed) {
                if (entry instanceof String term) {
                    preferences.addRecentSearch(term);
                }
            }
        }
        Object color = map.get("colorizedOutput");
        if (color instanceof Boolean bool) {
            preferences.setColorizedOutput(bool);
        }
        Object emoji = map.get("emojiOutput");
        if (emoji instanceof Boolean bool) {
            preferences.setEmojiOutput(bool);
        }
        Object achievements = map.get("acknowledgedAchievements");
        if (achievements instanceof List<?> list) {
            for (Object entry : list) {
                if (entry instanceof String id) {
                    preferences.acknowledgeAchievement(id);
                }
            }
        }
        // Easter egg: if you add "Order 66" to favourites the app still behaves... mostly.
    }
}
