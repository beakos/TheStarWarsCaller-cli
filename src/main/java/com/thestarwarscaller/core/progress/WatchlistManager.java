package com.thestarwarscaller.core.progress;

import com.thestarwarscaller.core.JsonParser;
import com.thestarwarscaller.core.JsonWriter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Handles persistence of the user's watchlist and viewing history. The manager keeps entries in a
 * LinkedHashMap so insertion order is preserved when we paginate results inside the CLI.
 */
public final class WatchlistManager {
    /** Formatter for YYYY-MM-DD dates stored on entries. */
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    /** Formatter for full timestamps recorded for history events. */
    private static final DateTimeFormatter DATE_TIME_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    /** Backing file on disk. */
    private final Path file;
    /** In-memory watchlist keyed by a normalised title. */
    private final Map<String, WatchlistEntry> entries;
    /** Rolling history of completed watch events. */
    private final List<WatchEvent> history;

    public WatchlistManager(Path file) {
        this.file = Objects.requireNonNull(file, "file");
        this.entries = new LinkedHashMap<>();
        this.history = new ArrayList<>();
    }

    /** Loads watchlist data from disk; safe to call repeatedly as it clears existing state. */
    public synchronized void load() throws IOException {
        // Start from a clean slate so repeated load() calls do not accumulate stale data.
        entries.clear();
        history.clear();
        if (!Files.exists(file)) {
            return;
        }
        // Parse the JSON payload using our lightweight parser.
        String json = Files.readString(file);
        Object parsed = JsonParser.parse(json);
        if (!(parsed instanceof Map<?, ?> root)) {
            // Bail out silently if the file is malformed; caller may choose to overwrite later.
            return;
        }
        // Rehydrate watchlist entries if present.
        Object watchlistNode = root.get("watchlist");
        if (watchlistNode instanceof List<?> list) {
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    WatchlistEntry entry = fromMap(castStringObject(map));
                    entries.put(normaliseKey(entry.title()), entry);
                }
            }
        }
        // Rehydrate history records if present.
        Object historyNode = root.get("history");
        if (historyNode instanceof List<?> list) {
            for (Object element : list) {
                if (element instanceof Map<?, ?> map) {
                    history.add(fromHistoryMap(castStringObject(map)));
                }
            }
        }
    }

    /** Persists the current watchlist and history back to disk in JSON form. */
    public synchronized void save() throws IOException {
        Map<String, Object> root = new LinkedHashMap<>();
        List<Map<String, Object>> watchlist = new ArrayList<>();
        for (WatchlistEntry entry : entries.values()) {
            watchlist.add(toMap(entry));
        }
        List<Map<String, Object>> historyList = new ArrayList<>();
        for (WatchEvent event : history) {
            historyList.add(toMap(event));
        }
        root.put("watchlist", watchlist);
        root.put("history", historyList);
        Files.createDirectories(file.getParent() == null ? Path.of(".") : file.getParent());
        Files.writeString(file, JsonWriter.write(root));
    }

    /** @return immutable snapshot of watchlist entries preserving insertion order. */
    public synchronized List<WatchlistEntry> entries() {
        return List.copyOf(entries.values());
    }

    /** @return immutable snapshot of the full viewing history. */
    public synchronized List<WatchEvent> history() {
        return List.copyOf(history);
    }

    /** Adds or updates an entry without changing watch progress. */
    public synchronized WatchlistEntry upsert(String title,
                                              WatchlistEntry.MediaType type,
                                              WatchlistEntry.Status status) {
        WatchlistEntry existing = entries.get(normaliseKey(title));
        WatchlistEntry updated = new WatchlistEntry(
                title,
                type,
                status,
                existing != null ? existing.rating() : 0,
                existing != null ? existing.timesWatched() : 0,
                existing != null ? existing.lastWatched() : null);
        entries.put(normaliseKey(title), updated);
        return updated;
    }

    /**
     * Marks an entry as completed, records a history event, and persists the change immediately.
     * A rating of -1 leaves the existing rating untouched.
     */
    public synchronized WatchlistEntry markCompleted(String title,
                                                     WatchlistEntry.MediaType type,
                                                     int rating,
                                                     String notes) throws IOException {
        String key = normaliseKey(title);
        WatchlistEntry existing = entries.get(key);
        int newTimesWatched = existing == null ? 1 : existing.timesWatched() + 1;
        int newRating = rating >= 0 ? rating : existing != null ? existing.rating() : 0;
        WatchlistEntry updated = new WatchlistEntry(
                title,
                type,
                WatchlistEntry.Status.COMPLETED,
                newRating,
                newTimesWatched,
                LocalDate.now());
        entries.put(key, updated);
        history.add(new WatchEvent(title, type, LocalDateTime.now(), newRating, notes));
        save();
        return updated;
    }

    /** Removes an entry from the watchlist while keeping historical records intact. */
    public synchronized void remove(String title) throws IOException {
        entries.remove(normaliseKey(title));
        save();
    }

    /** Ensures keys stay stable by using lower-case ASCII titles. */
    private static String normaliseKey(String title) {
        return title.toLowerCase(Locale.ROOT).trim();
    }

    private static Map<String, Object> toMap(WatchlistEntry entry) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", entry.title());
        map.put("type", entry.type().name());
        map.put("status", entry.status().name());
        map.put("rating", entry.rating());
        map.put("timesWatched", entry.timesWatched());
        map.put("lastWatched", entry.lastWatched() == null ? null : DATE_FORMAT.format(entry.lastWatched()));
        return map;
    }

    private static Map<String, Object> toMap(WatchEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", event.title());
        map.put("type", event.type().name());
        map.put("watchedAt", DATE_TIME_FORMAT.format(event.watchedAt()));
        map.put("rating", event.rating());
        map.put("notes", event.notes());
        return map;
    }

    private static WatchlistEntry fromMap(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");
        WatchlistEntry.MediaType type = WatchlistEntry.MediaType.valueOf(
                ((String) map.getOrDefault("type", WatchlistEntry.MediaType.MOVIE.name())).toUpperCase(Locale.ROOT));
        WatchlistEntry.Status status = WatchlistEntry.Status.valueOf(
                ((String) map.getOrDefault("status", WatchlistEntry.Status.PLANNED.name())).toUpperCase(Locale.ROOT));
        int rating = ((Number) map.getOrDefault("rating", 0)).intValue();
        int timesWatched = ((Number) map.getOrDefault("timesWatched", 0)).intValue();
        Object lastWatchedRaw = map.get("lastWatched");
        LocalDate lastWatched = lastWatchedRaw instanceof String str && !str.isBlank()
                ? LocalDate.parse(str, DATE_FORMAT)
                : null;
        return new WatchlistEntry(title, type, status, rating, timesWatched, lastWatched);
    }

    private static WatchEvent fromHistoryMap(Map<String, Object> map) {
        String title = (String) map.getOrDefault("title", "");
        WatchlistEntry.MediaType type = WatchlistEntry.MediaType.valueOf(
                ((String) map.getOrDefault("type", WatchlistEntry.MediaType.MOVIE.name())).toUpperCase(Locale.ROOT));
        LocalDateTime watchedAt = LocalDateTime.parse((String) map.get("watchedAt"), DATE_TIME_FORMAT);
        int rating = ((Number) map.getOrDefault("rating", 0)).intValue();
        String notes = (String) map.getOrDefault("notes", "");
        return new WatchEvent(title, type, watchedAt, rating, notes);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castStringObject(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    /** Utility that exposes watched titles for achievement calculations. */
    public synchronized Set<String> completedTitles() {
        Set<String> titles = new LinkedHashSet<>();
        for (WatchlistEntry entry : entries.values()) {
            if (entry.status() == WatchlistEntry.Status.COMPLETED) {
                titles.add(entry.title());
            }
        }
        return Set.copyOf(titles);
    }
}
