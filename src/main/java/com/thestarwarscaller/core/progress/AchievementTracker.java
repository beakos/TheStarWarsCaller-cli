package com.thestarwarscaller.core.progress;

import com.thestarwarscaller.core.MediaCatalog;
import com.thestarwarscaller.core.Movie;
import com.thestarwarscaller.core.MovieCategory;
import com.thestarwarscaller.core.Series;

import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Figures out which achievements the user has unlocked based on their watch history.
 * Percentages are returned so the CLI can show partial progress bars.
 */
public final class AchievementTracker {
    private final MediaCatalog catalog;

    public AchievementTracker(MediaCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /** Calculates fractional completion for each tracked achievement (0.0 - 1.0). */
    public Map<Achievement, Double> progress(Set<String> completedTitles) {
        Set<String> normalised = normalise(completedTitles);
        Map<Achievement, Double> progress = new EnumMap<>(Achievement.class);
        progress.put(Achievement.SAGA_COMPLETE, ratio(countMovies(MovieCategory.SAGA, normalised), totalMovies(MovieCategory.SAGA)));
        progress.put(Achievement.ANTHOLOGY_EXPLORER, ratio(countMovies(MovieCategory.ANTHOLOGY, normalised), totalMovies(MovieCategory.ANTHOLOGY)));
        progress.put(Achievement.LEGO_MASTER_BUILDER, ratio(countMovies(MovieCategory.LEGO_SPECIAL, normalised), totalMovies(MovieCategory.LEGO_SPECIAL)));
        progress.put(Achievement.CLONE_WARS_COMPLETIONIST, cloneWarsRatio(normalised));
        return progress;
    }

    /** @return achievements where the progress has reached or surpassed 100%. */
    public Set<Achievement> unlocked(Set<String> completedTitles) {
        Map<Achievement, Double> progress = progress(completedTitles);
        return progress.entrySet().stream()
                .filter(entry -> entry.getValue() >= 1.0 - 1e-9)
                .map(Map.Entry::getKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private double ratio(long completed, long total) {
        if (total == 0) {
            return 0.0;
        }
        return Math.min(1.0, (double) completed / total);
    }

    private long totalMovies(MovieCategory category) {
        return catalog.getMovies().stream()
                .filter(movie -> movie.getCategory() == category)
                .count();
    }

    private long countMovies(MovieCategory category, Set<String> completedNormalised) {
        return catalog.getMovies().stream()
                .filter(movie -> movie.getCategory() == category)
                .map(Movie::getTitle)
                .map(AchievementTracker::normalise)
                .filter(completedNormalised::contains)
                .count();
    }

    private double cloneWarsRatio(Set<String> completedNormalised) {
        long total = catalog.getMovies().stream()
                .map(Movie::getTitle)
                .filter(title -> title.toLowerCase(Locale.ROOT).contains("clone wars"))
                .count();
        total += catalog.getSeries().stream()
                .map(Series::getTitle)
                .filter(title -> title.toLowerCase(Locale.ROOT).contains("clone wars"))
                .count();
        if (total == 0) {
            return 0.0;
        }
        long completed = completedNormalised.stream()
                .filter(title -> title.contains("clone wars"))
                .count();
        return Math.min(1.0, (double) completed / total);
    }

    private static Set<String> normalise(Set<String> titles) {
        return titles.stream()
                .map(AchievementTracker::normalise)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalise(String title) {
        return title.toLowerCase(Locale.ROOT).trim();
    }
}
