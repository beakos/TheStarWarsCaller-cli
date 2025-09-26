package com.thestarwarscaller.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Central catalogue of all Star Wars films and series that the app knows about.
 * Think of this as the Jedi Archives where every query or filter is processed.
 */
public final class MediaCatalog {
    /** Immutable list of films available through the app. */
    private final List<Movie> movies;
    /** Immutable list of television/streaming series. */
    private final List<Series> series;

    /**
     * Builds a catalogue with the supplied movies and series.
     * Nothing fancy happens here: we just keep safe copies so the data stays immutable.
     */
    public MediaCatalog(List<Movie> movies, List<Series> series) {
        this.movies = List.copyOf(Objects.requireNonNull(movies, "movies"));
        this.series = List.copyOf(Objects.requireNonNull(series, "series"));
    }

    /** Returns every film we have recorded (ordered is handled elsewhere). */
    public List<Movie> getMovies() {
        return movies;
    }

    /** Returns every series we have recorded. */
    public List<Series> getSeries() {
        return series;
    }

    /**
     * Filters films by category (Saga vs Anthology etc.).
     * Useful when the user chooses a specific bucket from the menu.
     */
    public List<Movie> byCategory(MovieCategory category) {
        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.getCategory() == category) {
                results.add(movie);
            }
        }
        results.sort(Comparator.comparingInt(Movie::getReleaseYear));
        return results;
    }

    /**
     * Filters films by timeline era (e.g. "High Republic").
     * Perfect for building chronological watchlists.
     */
    public List<Movie> byEra(String era) {
        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies) {
            if (era.equals(movie.getEra())) {
                results.add(movie);
            }
        }
        results.sort(Comparator.comparingInt(Movie::getReleaseYear));
        return results;
    }

    /**
     * Runs a text search across titles, eras, and synopses for films.
     * Lower-case matching gives us case-insensitive results without extra libraries.
     */
    public List<Movie> searchMovies(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Movie> results = new ArrayList<>();
        for (Movie movie : movies) {
            if (movie.matches(query)) {
                results.add(movie);
            }
        }
        results.sort(Comparator.comparingInt(Movie::getReleaseYear));
        return results;
    }

    /** Filters series by format (live action, LEGO, etc.). */
    public List<Series> byFormat(SeriesFormat format) {
        List<Series> results = new ArrayList<>();
        for (Series show : series) {
            if (show.getFormat() == format) {
                results.add(show);
            }
        }
        results.sort(Comparator.comparingInt(Series::getStartYear));
        return results;
    }

    /** Filters series by era label. */
    public List<Series> bySeriesEra(String era) {
        List<Series> results = new ArrayList<>();
        for (Series show : series) {
            if (era.equals(show.getEra())) {
                results.add(show);
            }
        }
        results.sort(Comparator.comparingInt(Series::getStartYear));
        return results;
    }

    /**
     * Performs a text search across the series collection.
     * Handy when fans only remember a single character name or location.
     */
    public List<Series> searchSeries(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        List<Series> results = new ArrayList<>();
        String lower = query.toLowerCase(Locale.ROOT);
        for (Series show : series) {
            if (show.matches(lower)) {
                results.add(show);
            }
        }
        results.sort(Comparator.comparingInt(Series::getStartYear));
        return results;
    }

    /** Returns films sorted by release year ascending. */
    public List<Movie> sortedMovies() {
        List<Movie> sorted = new ArrayList<>(movies);
        sorted.sort(Comparator.comparingInt(Movie::getReleaseYear));
        return sorted;
    }

    /** Returns series sorted by first air year ascending. */
    public List<Series> sortedSeries() {
        List<Series> sorted = new ArrayList<>(series);
        sorted.sort(Comparator.comparingInt(Series::getStartYear));
        return sorted;
    }

    /**
     * Groups films into a map keyed by category.
     * TreeMap keeps keys in enum ordinal order so listings look tidy.
     */
    public Map<MovieCategory, List<Movie>> moviesGroupedByCategory() {
        Map<MovieCategory, List<Movie>> grouped = new TreeMap<>((a, b) -> a.ordinal() - b.ordinal());
        for (Movie movie : movies) {
            grouped.computeIfAbsent(movie.getCategory(), ignore -> new ArrayList<>()).add(movie);
        }
        for (List<Movie> value : grouped.values()) {
            value.sort(Comparator.comparingInt(Movie::getReleaseYear));
        }
        return grouped;
    }

    /** Same grouping trick for series by format. */
    public Map<SeriesFormat, List<Series>> seriesGroupedByFormat() {
        Map<SeriesFormat, List<Series>> grouped = new TreeMap<>((a, b) -> a.ordinal() - b.ordinal());
        for (Series show : series) {
            grouped.computeIfAbsent(show.getFormat(), ignore -> new ArrayList<>()).add(show);
        }
        for (List<Series> value : grouped.values()) {
            value.sort(Comparator.comparingInt(Series::getStartYear));
        }
        return grouped;
    }

    /** Extracts a sorted set of film eras for menu options. */
    public Set<String> movieEras() {
        Set<String> eras = new TreeSet<>();
        for (Movie movie : movies) {
            if (movie.getEra() != null && !movie.getEra().isBlank()) {
                eras.add(movie.getEra());
            }
        }
        return eras;
    }

    /** Extracts a sorted set of series eras. */
    public Set<String> seriesEras() {
        Set<String> eras = new TreeSet<>();
        for (Series show : series) {
            if (show.getEra() != null && !show.getEra().isBlank()) {
                eras.add(show.getEra());
            }
        }
        return eras;
    }

    /** Helper used by the CLI to export film selections to JSON. */
    public String exportMoviesAsJson(Collection<Movie> selection) {
        return JsonExporter.moviesToJson(selection);
    }

    /** Helper used by the CLI to export series selections to JSON. */
    public String exportSeriesAsJson(Collection<Series> selection) {
        return JsonExporter.seriesToJson(selection);
    }
}
