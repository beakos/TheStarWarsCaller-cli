package com.thestarwarscaller.core.story;

import com.thestarwarscaller.core.Movie;
import com.thestarwarscaller.core.lore.CharacterProfile;
import com.thestarwarscaller.core.lore.LoreRepository;
import com.thestarwarscaller.core.lore.TimelineEvent;
import com.thestarwarscaller.core.sync.ExternalMetadataSyncService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/** Builds narrative briefings that weave together movie data, lore, and external metadata. */
public final class StoryBriefingBuilder {

    public StoryBriefingBuilder() {
        // Stateless helper – no dependencies to wire up.
    }

    /**
     * Generates a multi-paragraph story synopsis for the supplied film.
     * @param movie the film to summarise
     * @param lore lore repository that links characters, planets, and timeline events
     * @param snapshot optional external metadata (may be {@code null} if offline)
     */
    public StoryBriefing build(Movie movie,
                               LoreRepository lore,
                               ExternalMetadataSyncService.MetadataSnapshot snapshot) {
        Objects.requireNonNull(movie, "movie");
        List<String> paragraphs = new ArrayList<>();

        paragraphs.add(introParagraph(movie));
        paragraphs.addAll(characterParagraphs(movie, lore));
        paragraphs.addAll(timelineParagraphs(movie, lore));
        externalMetadataParagraph(movie, snapshot).ifPresent(paragraphs::add);
        notesParagraph(movie).ifPresent(paragraphs::add);

        String title = "Story Briefing: " + movie.getTitle();
        return new StoryBriefing(title, paragraphs);
    }

    private String introParagraph(Movie movie) {
        StringBuilder sb = new StringBuilder();
        sb.append("Set during the ").append(movie.getEra() == null ? "unknown era" : movie.getEra());
        sb.append(", this adventure premieres in ").append(movie.getReleaseYear());
        sb.append(" and belongs to the ").append(movie.getCategory().getDisplayName()).append('.');
        if (movie.getSynopsis() != null && !movie.getSynopsis().isBlank()) {
            sb.append(' ').append(movie.getSynopsis());
        }
        if (movie.getStreaming() != null && !movie.getStreaming().isBlank()) {
            sb.append(" Stream it via ").append(movie.getStreaming()).append('.');
        }
        return sb.toString();
    }

    private List<String> characterParagraphs(Movie movie, LoreRepository lore) {
        List<String> paragraphs = new ArrayList<>();
        List<CharacterProfile> featured = lore.characters().stream()
                .filter(profile -> profile.mediaAppearances().stream().anyMatch(media -> equalsIgnoreCase(media, movie.getTitle())))
                .toList();
        if (!featured.isEmpty()) {
            StringBuilder sb = new StringBuilder("Key characters: ");
            for (int i = 0; i < featured.size(); i++) {
                CharacterProfile profile = featured.get(i);
                sb.append(profile.name());
                if (!profile.aliases().isEmpty()) {
                    sb.append(" (a.k.a. ").append(String.join(", ", profile.aliases())).append(')');
                }
                if (i + 1 < featured.size()) {
                    sb.append(", ");
                }
            }
            paragraphs.add(sb.append('.').toString());
        }
        return paragraphs;
    }

    private List<String> timelineParagraphs(Movie movie, LoreRepository lore) {
        List<String> paragraphs = new ArrayList<>();
        List<TimelineEvent> events = lore.events().stream()
                .filter(event -> event.mediaReferences().stream().anyMatch(media -> equalsIgnoreCase(media, movie.getTitle())))
                .toList();
        if (!events.isEmpty()) {
            StringBuilder sb = new StringBuilder("Timeline beats connected to this story: ");
            for (int i = 0; i < events.size(); i++) {
                TimelineEvent event = events.get(i);
                sb.append(event.title()).append(" (").append(event.era()).append(')');
                if (i + 1 < events.size()) {
                    sb.append("; ");
                }
            }
            paragraphs.add(sb.append('.').toString());
        }
        return paragraphs;
    }

    private Optional<String> externalMetadataParagraph(Movie movie,
                                                       ExternalMetadataSyncService.MetadataSnapshot snapshot) {
        if (snapshot == null) {
            return Optional.empty();
        }
        Object results = snapshot.swapiFilms().get("results");
        if (!(results instanceof List<?> list)) {
            return Optional.empty();
        }
        for (Object element : list) {
            if (element instanceof Map<?, ?> map) {
                Object title = map.get("title");
                if (title instanceof String str && equalsIgnoreCase(str, movie.getTitle())) {
                    Object releaseDate = map.get("release_date");
                    Object crawl = map.get("opening_crawl");
                    StringBuilder sb = new StringBuilder("SWAPI notes: ");
                    if (releaseDate instanceof String rd && !rd.isBlank()) {
                        sb.append("released on ").append(rd).append('.');
                    }
                    if (crawl instanceof String oc && !oc.isBlank()) {
                        sb.append(' ').append(oc.replace('\r', ' ').replace('\n', ' '));
                    }
                    return Optional.of(sb.toString());
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> notesParagraph(Movie movie) {
        if (movie.getNotes() == null || movie.getNotes().isBlank()) {
            return Optional.empty();
        }
        return Optional.of("Trivia transmission: " + movie.getNotes());
    }

    private boolean equalsIgnoreCase(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
