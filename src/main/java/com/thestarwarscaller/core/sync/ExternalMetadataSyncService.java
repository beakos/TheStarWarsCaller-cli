package com.thestarwarscaller.core.sync;

import com.thestarwarscaller.core.JsonParser;
import com.thestarwarscaller.core.JsonWriter;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Periodically synchronises metadata with public APIs (SWAPI today, other feeds tomorrow) and keeps
 * a cache on disk so offline sessions still have something to fall back to.
 */
public final class ExternalMetadataSyncService {
    private static final URI SWAPI_FILMS = URI.create("https://swapi.dev/api/films/");
    private static final String CACHE_FILE = "metadata-cache.json";
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private final HttpClient client;
    private final Path cacheDir;

    public ExternalMetadataSyncService(Path cacheDir) {
        this(HttpClient.newHttpClient(), cacheDir);
    }

    ExternalMetadataSyncService(HttpClient client, Path cacheDir) {
        this.client = Objects.requireNonNull(client, "client");
        this.cacheDir = Objects.requireNonNull(cacheDir, "cacheDir");
    }

    /** Pulls fresh data from SWAPI and persists the snapshot locally. */
    public MetadataSnapshot refreshNow() throws IOException, InterruptedException {
        Map<String, Object> swapiFilms = fetchSwapiFilms();
        MetadataSnapshot snapshot = new MetadataSnapshot(LocalDateTime.now(), swapiFilms);
        writeCache(snapshot);
        return snapshot;
    }

    /** Loads the last cached snapshot from disk, or {@code null} if none exists. */
    public MetadataSnapshot loadSnapshot() throws IOException {
        Path file = cacheDir.resolve(CACHE_FILE);
        if (!Files.exists(file)) {
            return null;
        }
        Object parsed = JsonParser.parse(Files.readString(file, StandardCharsets.UTF_8));
        if (!(parsed instanceof Map<?, ?> map)) {
            return null;
        }
        Object timestamp = map.get("fetchedAt");
        Object payload = map.get("swapiFilms");
        if (!(timestamp instanceof String ts) || !(payload instanceof Map<?, ?> p)) {
            return null;
        }
        return new MetadataSnapshot(LocalDateTime.parse(ts, FORMATTER), castStringObject(p));
    }

    private Map<String, Object> fetchSwapiFilms() throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(SWAPI_FILMS)
                .header("User-Agent", "TheStarWarsCaller/cli")
                .GET()
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IOException("Failed to fetch SWAPI films: " + response.statusCode());
        }
        Object parsed = JsonParser.parse(response.body());
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IOException("Unexpected SWAPI payload format");
        }
        return castStringObject(map);
    }

    private void writeCache(MetadataSnapshot snapshot) throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("fetchedAt", FORMATTER.format(snapshot.fetchedAt()));
        root.put("swapiFilms", snapshot.swapiFilms());
        Files.createDirectories(cacheDir);
        Files.writeString(cacheDir.resolve(CACHE_FILE), JsonWriter.write(root), StandardCharsets.UTF_8);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castStringObject(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    /** Simple DTO that callers can feed into view models or persistence layers. */
    public record MetadataSnapshot(LocalDateTime fetchedAt,
                                   Map<String, Object> swapiFilms) {
    }
}
