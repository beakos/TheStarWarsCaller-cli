package com.thestarwarscaller.core;

import java.nio.file.Path;
import java.util.Map;
import java.util.Objects;

/**
 * Holds basic configuration like where data files live.
 * Think of this as the datapad that tells the application which crates to open.
 */
public final class AppConfig {
    private final Path dataDirectory;
    private final Path moviesFile;
    private final Path seriesFile;
    private final Path preferencesFile;

    public AppConfig(Path dataDirectory, Path moviesFile, Path seriesFile, Path preferencesFile) {
        this.dataDirectory = Objects.requireNonNull(dataDirectory, "dataDirectory");
        this.moviesFile = Objects.requireNonNull(moviesFile, "moviesFile");
        this.seriesFile = Objects.requireNonNull(seriesFile, "seriesFile");
        this.preferencesFile = Objects.requireNonNull(preferencesFile, "preferencesFile");
    }

    /** @return directory that contains the media JSON files. */
    public Path getDataDirectory() {
        return dataDirectory;
    }

    /** @return path to the movies JSON file. */
    public Path getMoviesFile() {
        return moviesFile;
    }

    /** @return path to the series JSON file. */
    public Path getSeriesFile() {
        return seriesFile;
    }

    /** @return path where user preferences are stored. */
    public Path getPreferencesFile() {
        return preferencesFile;
    }

    /** Builds an {@link AppConfig} from a parsed JSON map with optional overrides. */
    static AppConfig fromMap(Map<String, Object> map, Path basePath) {
        Path dataDir = basePath.resolve(getString(map, "dataDir", "data"));
        Path moviesPath = dataDir.resolve(getString(map, "moviesFile", "movies.json"));
        Path seriesPath = dataDir.resolve(getString(map, "seriesFile", "series.json"));
        Path preferencesPath = basePath.resolve(getString(map, "preferencesFile", "user-preferences.json"));
        return new AppConfig(dataDir, moviesPath, seriesPath, preferencesPath);
    }

    /** Helper for pulling optional string values with a default. */
    private static String getString(Map<String, Object> map, String key, String defaultValue) {
        Object value = map.get(key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof String string) {
            return string;
        }
        throw new IllegalArgumentException("App config field '" + key + "' must be a string");
    }
}
