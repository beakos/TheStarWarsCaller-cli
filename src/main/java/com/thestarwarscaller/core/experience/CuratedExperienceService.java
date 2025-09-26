package com.thestarwarscaller.core.experience;

import com.thestarwarscaller.core.MediaCatalog;
import com.thestarwarscaller.core.Movie;
import com.thestarwarscaller.core.MovieCategory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * Generates curated movie groupings: random missions, chronological marathons, and double features.
 * The selection logic uses lightweight heuristics so results feel themed rather than purely random.
 */
public final class CuratedExperienceService {
    private final MediaCatalog catalog;
    private final Random random;

    public CuratedExperienceService(MediaCatalog catalog) {
        this(catalog, ThreadLocalRandom.current());
    }

    public CuratedExperienceService(MediaCatalog catalog, Random random) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
        this.random = Objects.requireNonNull(random, "random");
    }

    /** Picks a weighted set of adventures balancing runtime and category variety. */
    public ExperienceSuggestion buildRandomMission(int count) {
        List<Movie> pool = new ArrayList<>(catalog.getMovies());
        List<Movie> picks = new ArrayList<>();
        while (!pool.isEmpty() && picks.size() < count) {
            double totalWeight = pool.stream().mapToDouble(this::weight).sum();
            double roll = random.nextDouble(totalWeight);
            double cursor = 0.0;
            Movie chosen = null;
            for (Movie movie : pool) {
                cursor += weight(movie);
                if (cursor >= roll) {
                    chosen = movie;
                    break;
                }
            }
            if (chosen == null) {
                chosen = pool.get(pool.size() - 1);
            }
            picks.add(chosen);
            pool.remove(chosen);
        }
        String description = "A surprise lineup pulling from every corner of the galaxy.";
        return new ExperienceSuggestion("Random Mission", description, picks);
    }

    /** Builds a chronological playlist capped at the requested length. */
    public ExperienceSuggestion buildChronologicalMarathon(int limit) {
        List<Movie> ordered = new ArrayList<>(catalog.getMovies());
        ordered.sort(Comparator.comparingInt(Movie::getReleaseYear));
        if (limit > 0 && ordered.size() > limit) {
            ordered = ordered.subList(0, limit);
        }
        String description = String.format(Locale.ROOT,
                "Chronological marathon featuring %d key holos", ordered.size());
        return new ExperienceSuggestion("Chronological Marathon", description, List.copyOf(ordered));
    }

    /** Offers two thematically linked films that pair nicely for an evening. */
    public ExperienceSuggestion buildDoubleFeature() {
        List<Movie> movies = new ArrayList<>(catalog.getMovies());
        if (movies.size() < 2) {
            return new ExperienceSuggestion("Double Feature", "Not enough films to build a pairing.", movies);
        }
        Map<String, List<Movie>> byEra = movies.stream()
                .filter(movie -> movie.getEra() != null)
                .collect(Collectors.groupingBy(Movie::getEra));
        List<Movie> pairing = new ArrayList<>();
        String theme = "Complementary eras";
        for (Map.Entry<String, List<Movie>> entry : byEra.entrySet()) {
            if (entry.getValue().size() >= 2) {
                List<Movie> eraMovies = new ArrayList<>(entry.getValue());
                eraMovies.sort(Comparator.comparingInt(Movie::getReleaseYear));
                pairing.add(eraMovies.get(0));
                pairing.add(eraMovies.get(1));
                theme = "Stories set during the " + entry.getKey();
                break;
            }
        }
        if (pairing.isEmpty()) {
            movies.sort(Comparator.comparingInt(Movie::getReleaseYear));
            pairing.add(movies.get(0));
            pairing.add(movies.get(1));
            theme = "Back-to-back classics from the archives";
        }
        return new ExperienceSuggestion("Tonight's Double Feature", theme, List.copyOf(pairing));
    }

    private double weight(Movie movie) {
        double categoryBias = switch (movie.getCategory()) {
            case SAGA -> 0.9;
            case ANTHOLOGY -> 1.1;
            case ANIMATED_FEATURE -> 1.2;
            case TV_FILM -> 1.3;
            case LEGO_SPECIAL -> 1.4;
        };
        double runtimeBias = 180.0 / estimateRuntime(movie);
        return categoryBias * runtimeBias;
    }

    private int estimateRuntime(Movie movie) {
        return switch (movie.getCategory()) {
            case SAGA -> 135;
            case ANTHOLOGY -> 125;
            case ANIMATED_FEATURE -> 98;
            case TV_FILM -> 90;
            case LEGO_SPECIAL -> 45;
        };
    }
}
