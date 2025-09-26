package com.thestarwarscaller.core.lore;

import com.thestarwarscaller.core.JsonParser;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Central index for lore data: characters, planets, and key timeline events. The repository loads
 * from a JSON file when available, but also ships with a curated in-memory dataset so fresh installs
 * have something to explore immediately.
 */
public final class LoreRepository {
    private final List<CharacterProfile> characters;
    private final List<PlanetProfile> planets;
    private final List<TimelineEvent> events;

    public LoreRepository(List<CharacterProfile> characters,
                          List<PlanetProfile> planets,
                          List<TimelineEvent> events) {
        this.characters = List.copyOf(Objects.requireNonNull(characters, "characters"));
        this.planets = List.copyOf(Objects.requireNonNull(planets, "planets"));
        this.events = List.copyOf(Objects.requireNonNull(events, "events"));
    }

    /** Attempts to load data from disk, falling back to a bundled dataset if the file is missing. */
    public static LoreRepository loadOrDefault(Path file) throws IOException {
        if (file != null && Files.exists(file)) {
            return fromJson(Files.readString(file));
        }
        return bundled();
    }

    /** Parses a JSON document into a repository instance. */
    public static LoreRepository fromJson(String json) {
        Object parsed = JsonParser.parse(json);
        if (!(parsed instanceof Map<?, ?> map)) {
            throw new IllegalArgumentException("Lore file must be a JSON object");
        }
        return new LoreRepository(
                parseCharacters(map.get("characters")),
                parsePlanets(map.get("planets")),
                parseEvents(map.get("timeline")));
    }

    /** @return the curated dataset bundled with the CLI. */
    public static LoreRepository bundled() {
        List<CharacterProfile> characters = List.of(
                new CharacterProfile(
                        "Ahsoka Tano",
                        List.of("Snips", "Fulcrum"),
                        List.of("Jedi Order", "Fulcrum", "Rebel Alliance"),
                        "Shili",
                        "Former Padawan of Anakin Skywalker who becomes a key Rebel operative.",
                        List.of("Star Wars: The Clone Wars", "Star Wars Rebels", "The Mandalorian")),
                new CharacterProfile(
                        "Luke Skywalker",
                        List.of("Red Five"),
                        List.of("Rebel Alliance", "Jedi Order"),
                        "Tatooine",
                        "Farmboy turned Jedi who redeems his father and rebuilds the Order.",
                        List.of("A New Hope", "The Empire Strikes Back", "Return of the Jedi", "The Last Jedi")));
        List<PlanetProfile> planets = List.of(
                new PlanetProfile(
                        "Coruscant",
                        "Core Worlds",
                        "Ecumenopolis capital of the Republic and later the Galactic Empire.",
                        List.of("Battle of Coruscant", "Order 66"),
                        List.of("Attack of the Clones", "Revenge of the Sith", "The Clone Wars")),
                new PlanetProfile(
                        "Mandalore",
                        "Outer Rim",
                        "War-torn homeworld of the Mandalorians, defined by clan politics.",
                        List.of("Siege of Mandalore", "Purge of Mandalore"),
                        List.of("The Clone Wars", "The Mandalorian", "The Book of Boba Fett")));
        List<TimelineEvent> events = List.of(
                new TimelineEvent(
                        "Siege of Mandalore",
                        "Clone Wars",
                        -19,
                        "Ahsoka Tano and Clone Captain Rex lead a campaign to liberate Mandalore from Maul.",
                        List.of("Ahsoka Tano", "Maul", "Rex"),
                        List.of("Mandalore"),
                        List.of("Star Wars: The Clone Wars")),
                new TimelineEvent(
                        "Battle of Endor",
                        "Galactic Civil War",
                        4,
                        "Rebel Alliance destroys the second Death Star and topples Palpatine's rule.",
                        List.of("Luke Skywalker", "Leia Organa", "Han Solo"),
                        List.of("Endor"),
                        List.of("Return of the Jedi")));
        return new LoreRepository(characters, planets, events);
    }

    /** Finds characters whose name or alias includes the query (case-insensitive). */
    public List<CharacterProfile> findCharacters(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return characters.stream()
                .filter(profile -> matches(profile.name(), needle) || profile.aliases().stream().anyMatch(alias -> matches(alias, needle)))
                .collect(Collectors.toList());
    }

    /** Finds planets containing the query in their name or description. */
    public List<PlanetProfile> findPlanets(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String needle = query.toLowerCase(Locale.ROOT);
        return planets.stream()
                .filter(planet -> matches(planet.name(), needle)
                        || matches(planet.description(), needle)
                        || planet.notableEvents().stream().anyMatch(event -> matches(event, needle)))
                .collect(Collectors.toList());
    }

    /** Returns every timeline event where the given character participates. */
    public List<TimelineEvent> eventsFeaturingCharacter(String characterName) {
        if (characterName == null || characterName.isBlank()) {
            return List.of();
        }
        String needle = characterName.toLowerCase(Locale.ROOT);
        return events.stream()
                .filter(event -> event.involvedCharacters().stream().anyMatch(name -> name.toLowerCase(Locale.ROOT).contains(needle)))
                .collect(Collectors.toList());
    }

    /** Returns timeline events that take place on the supplied planet. */
    public List<TimelineEvent> eventsAtLocation(String location) {
        if (location == null || location.isBlank()) {
            return List.of();
        }
        String needle = location.toLowerCase(Locale.ROOT);
        return events.stream()
                .filter(event -> event.locations().stream().anyMatch(place -> place.toLowerCase(Locale.ROOT).contains(needle)))
                .collect(Collectors.toList());
    }

    /** Provides an immutable view of all characters for menu listings. */
    public List<CharacterProfile> characters() {
        return characters;
    }

    /** Provides an immutable view of all planets for menu listings. */
    public List<PlanetProfile> planets() {
        return planets;
    }

    /** Provides an immutable view of all timeline events. */
    public List<TimelineEvent> events() {
        return events;
    }

    private static boolean matches(String haystack, String needle) {
        return haystack != null && haystack.toLowerCase(Locale.ROOT).contains(needle);
    }

    @SuppressWarnings("unchecked")
    private static List<CharacterProfile> parseCharacters(Object node) {
        if (!(node instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<CharacterProfile> profiles = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                Map<String, Object> cast = (Map<String, Object>) map;
                profiles.add(new CharacterProfile(
                        (String) cast.getOrDefault("name", ""),
                        asStringList(cast.get("aliases")),
                        asStringList(cast.get("affiliations")),
                        (String) cast.getOrDefault("homeworld", ""),
                        (String) cast.getOrDefault("biography", ""),
                        asStringList(cast.get("media"))));
            }
        }
        return profiles;
    }

    @SuppressWarnings("unchecked")
    private static List<PlanetProfile> parsePlanets(Object node) {
        if (!(node instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<PlanetProfile> profiles = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                Map<String, Object> cast = (Map<String, Object>) map;
                profiles.add(new PlanetProfile(
                        (String) cast.getOrDefault("name", ""),
                        (String) cast.getOrDefault("region", ""),
                        (String) cast.getOrDefault("description", ""),
                        asStringList(cast.get("events")),
                        asStringList(cast.get("media"))));
            }
        }
        return profiles;
    }

    @SuppressWarnings("unchecked")
    private static List<TimelineEvent> parseEvents(Object node) {
        if (!(node instanceof List<?> list)) {
            return Collections.emptyList();
        }
        List<TimelineEvent> events = new ArrayList<>();
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                Map<String, Object> cast = (Map<String, Object>) map;
                events.add(new TimelineEvent(
                        (String) cast.getOrDefault("title", ""),
                        (String) cast.getOrDefault("era", ""),
                        ((Number) cast.getOrDefault("year", 0)).intValue(),
                        (String) cast.getOrDefault("description", ""),
                        asStringList(cast.get("characters")),
                        asStringList(cast.get("locations")),
                        asStringList(cast.get("media"))));
            }
        }
        return events;
    }

    @SuppressWarnings("unchecked")
    private static List<String> asStringList(Object node) {
        if (!(node instanceof List<?> list)) {
            return List.of();
        }
        List<String> strings = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof String str) {
                strings.add(str);
            }
        }
        return List.copyOf(strings);
    }

    /** Serialises the repository into a JSON-friendly map structure. */
    public Map<String, Object> toMap() {
        Map<String, Object> root = new LinkedHashMap<>();
        root.put("characters", characters.stream().map(LoreRepository::characterToMap).collect(Collectors.toList()));
        root.put("planets", planets.stream().map(LoreRepository::planetToMap).collect(Collectors.toList()));
        root.put("timeline", events.stream().map(LoreRepository::eventToMap).collect(Collectors.toList()));
        return root;
    }

    private static Map<String, Object> characterToMap(CharacterProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", profile.name());
        map.put("aliases", profile.aliases());
        map.put("affiliations", profile.affiliations());
        map.put("homeworld", profile.homeworld());
        map.put("biography", profile.biography());
        map.put("media", profile.mediaAppearances());
        return map;
    }

    private static Map<String, Object> planetToMap(PlanetProfile profile) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", profile.name());
        map.put("region", profile.region());
        map.put("description", profile.description());
        map.put("events", profile.notableEvents());
        map.put("media", profile.featuredIn());
        return map;
    }

    private static Map<String, Object> eventToMap(TimelineEvent event) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("title", event.title());
        map.put("era", event.era());
        map.put("year", event.year());
        map.put("description", event.description());
        map.put("characters", event.involvedCharacters());
        map.put("locations", event.locations());
        map.put("media", event.mediaReferences());
        return map;
    }
}
