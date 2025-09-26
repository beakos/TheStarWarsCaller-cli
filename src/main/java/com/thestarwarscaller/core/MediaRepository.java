package com.thestarwarscaller.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads media data from JSON files and turns them into Java objects.
 * Think of this class as the cargo droid that unloads data crates into our catalog.
 */
public final class MediaRepository {
    private MediaRepository() {
        // Static helper only; no need to instantiate (Jedi mind trick works fine).
    }

    /**
     * Loads both movies and series into a {@link MediaCatalog} using the supplied paths.
     */
    public static MediaCatalog loadCatalog(Path moviesPath, Path seriesPath) throws IOException {
        List<Movie> movies = loadMovies(moviesPath);
        List<Series> series = loadSeries(seriesPath);
        return new MediaCatalog(movies, series);
    }

    /**
     * Reads the movie JSON file and converts each entry into a {@link Movie} instance.
     */
    @SuppressWarnings("unchecked")
    public static List<Movie> loadMovies(Path path) throws IOException {
        String json = Files.readString(path);
        Object parsed = JsonParser.parse(json);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Movies file must be a JSON object");
        }
        Object moviesNode = root.get("movies");
        if (!(moviesNode instanceof List<?> moviesList)) {
            throw new IllegalArgumentException("Movies file requires a 'movies' array");
        }
        List<Movie> movies = new ArrayList<>();
        for (Object entry : moviesList) {
            if (!(entry instanceof Map<?, ?> movieMap)) {
                throw new IllegalArgumentException("Movie entry must be an object");
            }
            movies.add(mapToMovie((Map<String, Object>) movieMap));
        }
        return List.copyOf(movies);
    }

    /**
     * Reads the series JSON file and converts entries into {@link Series} objects.
     */
    @SuppressWarnings("unchecked")
    public static List<Series> loadSeries(Path path) throws IOException {
        String json = Files.readString(path);
        Object parsed = JsonParser.parse(json);
        if (!(parsed instanceof Map<?, ?> root)) {
            throw new IllegalArgumentException("Series file must be a JSON object");
        }
        Object seriesNode = root.get("series");
        if (!(seriesNode instanceof List<?> seriesList)) {
            throw new IllegalArgumentException("Series file requires a 'series' array");
        }
        List<Series> series = new ArrayList<>();
        for (Object entry : seriesList) {
            if (!(entry instanceof Map<?, ?> seriesMap)) {
                throw new IllegalArgumentException("Series entry must be an object");
            }
            series.add(mapToSeries((Map<String, Object>) seriesMap));
        }
        return List.copyOf(series);
    }

    /** Creates a {@link Movie} from the raw JSON map. */
    private static Movie mapToMovie(Map<String, Object> map) {
        String title = getString(map, "title", true);
        Integer episodeNumber = getInteger(map, "episodeNumber", false);
        String episodeRoman = getString(map, "episodeRoman", false);
        Integer releaseYear = getInteger(map, "releaseYear", true);
        String categoryValue = getString(map, "category", true);
        MovieCategory category = MovieCategory.valueOf(categoryValue);
        String era = getString(map, "era", false);
        String synopsis = getString(map, "synopsis", false);
        String streaming = getString(map, "streaming", false);
        String notes = getString(map, "notes", false);
        return new Movie(title, episodeNumber, episodeRoman, releaseYear, category, era, synopsis, streaming, notes);
    }

    /** Creates a {@link Series} from the raw JSON map. */
    private static Series mapToSeries(Map<String, Object> map) {
        String title = getString(map, "title", true);
        Integer startYear = getInteger(map, "startYear", true);
        Integer endYear = getInteger(map, "endYear", false);
        String formatValue = getString(map, "format", true);
        SeriesFormat format = SeriesFormat.valueOf(formatValue);
        String era = getString(map, "era", false);
        String synopsis = getString(map, "synopsis", false);
        String streaming = getString(map, "streaming", false);
        String notes = getString(map, "notes", false);
        return new Series(title, startYear, endYear, format, era, synopsis, streaming, notes);
    }

    /** Fetches a string from the map while validating its type and presence. */
    private static String getString(Map<String, Object> map, String key, boolean required) {
        Object value = map.get(key);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("Missing required string: " + key);
            }
            return null;
        }
        if (!(value instanceof String string)) {
            throw new IllegalArgumentException("Field '" + key + "' must be a string");
        }
        return string;
    }

    /** Fetches a number and converts it into an {@link Integer}. */
    private static Integer getInteger(Map<String, Object> map, String key, boolean required) {
        Object value = map.get(key);
        if (value == null) {
            if (required) {
                throw new IllegalArgumentException("Missing required number: " + key);
            }
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        throw new IllegalArgumentException("Field '" + key + "' must be a number");
    }
}
