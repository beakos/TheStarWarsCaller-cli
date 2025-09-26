package com.thestarwarscaller.core;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

/**
 * Reads the application configuration file from disk.
 * Imagine this class as the astromech that tells the ship which coordinates to plot.
 */
public final class ConfigManager {
    private ConfigManager() {
    }

    /**
     * Loads configuration from the provided path, or returns defaults if the file is missing.
     */
    public static AppConfig load(Path configPath) throws IOException {
        if (!Files.exists(configPath)) {
            return AppConfig.fromMap(Map.of(), configPath.getParent() != null ? configPath.getParent() : Path.of("."));
        }
        String json = Files.readString(configPath);
        Object parsed = JsonParser.parse(json);
        if (parsed instanceof Map<?, ?> map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cast = (Map<String, Object>) map;
            return AppConfig.fromMap(cast, configPath.getParent() != null ? configPath.getParent() : Path.of("."));
        }
        throw new IllegalArgumentException("Config file must be a JSON object");
    }
}
