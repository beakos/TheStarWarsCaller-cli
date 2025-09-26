package com.thestarwarscaller.core.search;

import com.thestarwarscaller.core.MediaCatalog;
import com.thestarwarscaller.core.Movie;
import com.thestarwarscaller.core.MovieCategory;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Performs fuzzy, typo-tolerant searches across the media catalogue. The engine takes into account
 * alternate spellings, stripped punctuation, and voice-assistant-friendly aliases so fans can say
 * "empire strikes back" and still land on the correct entry without needing perfect casing.
 */
public final class FuzzySearchEngine {
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^a-z0-9 ]+");
    private static final Pattern MULTI_SPACE = Pattern.compile("\\s+");
    private static final double MINIMUM_SCORE = 0.32;

    private final MediaCatalog catalog;

    public FuzzySearchEngine(MediaCatalog catalog) {
        this.catalog = Objects.requireNonNull(catalog, "catalog");
    }

    /**
     * Runs a fuzzy search and returns the top ranked movie matches. Results include the raw score
     * and the field we matched against so the CLI can display helpful hints.
     */
    public List<SearchResult<Movie>> searchMovies(String query, int limit) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String normalizedQuery = normalize(query);
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }
        List<SearchResult<Movie>> hits = new ArrayList<>();
        for (Movie movie : catalog.getMovies()) {
            MatchScore score = scoreMovie(normalizedQuery, movie);
            if (score.value >= MINIMUM_SCORE) {
                hits.add(new SearchResult<>(movie, score.value, score.matchedField));
            }
        }
        hits.sort(Comparator.comparingDouble(SearchResult<Movie>::score).reversed());
        if (limit > 0 && hits.size() > limit) {
            return List.copyOf(hits.subList(0, limit));
        }
        return List.copyOf(hits);
    }

    /**
     * Supplies suggestion strings even when nothing matches directly. Titles are deduplicated and
     * ordered by the fuzzy score so voice-driven interfaces can read them back to the user.
     */
    public List<String> suggestMovieTitles(String query, int limit) {
        List<SearchResult<Movie>> results = searchMovies(query, limit <= 0 ? 5 : limit);
        Set<String> seen = new HashSet<>();
        List<String> suggestions = new ArrayList<>();
        for (SearchResult<Movie> result : results) {
            String title = result.value().getTitle();
            if (seen.add(title)) {
                suggestions.add(title);
            }
        }
        return Collections.unmodifiableList(suggestions);
    }

    /** Calculates the best fuzzy score against all known aliases for a movie. */
    private MatchScore scoreMovie(String normalizedQuery, Movie movie) {
        double bestScore = 0.0;
        String bestField = "";
        for (String candidate : buildAliases(movie)) {
            double candidateScore = similarity(normalizedQuery, candidate);
            if (candidateScore > bestScore) {
                bestScore = candidateScore;
                bestField = candidate;
            }
        }
        // Small bias making favourites like saga films rank higher when ties occur.
        bestScore += categoryBias(movie.getCategory());
        return new MatchScore(bestField, Math.min(bestScore, 1.0));
    }

    /**
     * Generates alternate strings representing a movie including trimmed subtitles, roman numerals,
     * category names, and timeline eras. All strings are normalised to make downstream scoring fast.
     */
    private static Set<String> buildAliases(Movie movie) {
        Set<String> aliases = new HashSet<>();
        aliases.add(normalize(movie.getTitle()));
        if (movie.getEpisodeRoman() != null) {
            aliases.add(normalize("episode " + movie.getEpisodeRoman()));
            aliases.add(normalize(romanToArabic(movie.getEpisodeRoman())));
        }
        if (movie.getEpisodeNumber() != null) {
            aliases.add(normalize("episode " + movie.getEpisodeNumber()));
        }
        if (movie.getEra() != null) {
            aliases.add(normalize(movie.getEra()));
        }
        if (movie.getSynopsis() != null) {
            aliases.add(normalize(movie.getSynopsis()));
        }
        if (movie.getNotes() != null) {
            aliases.add(normalize(movie.getNotes()));
        }
        // Also split compound titles like "Star Wars: Episode V - The Empire Strikes Back".
        for (String piece : movie.getTitle().split("[:-]")) {
            String cleaned = normalize(piece);
            if (!cleaned.isEmpty()) {
                aliases.add(cleaned);
            }
        }
        // Voice friendly variant stripping "star wars" prefix entirely.
        String withoutPrefix = movie.getTitle().replaceFirst("(?i)star\\s+wars[: ]?", "");
        String cleaned = normalize(withoutPrefix);
        if (!cleaned.isEmpty()) {
            aliases.add(cleaned);
        }
        return aliases;
    }

    /** Applies lower-casing, accent stripping, roman numeral conversion, and punctuation trimming. */
    private static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String lower = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT);
        lower = romanWordsToDigits(lower);
        lower = NON_ALPHANUMERIC.matcher(lower).replaceAll(" ");
        lower = MULTI_SPACE.matcher(lower).replaceAll(" ").trim();
        return lower;
    }

    /** Converts strings such as "episode five" or "episode v" into numeric equivalents. */
    private static String romanWordsToDigits(String input) {
        String converted = input
                .replace("episode one", "episode 1")
                .replace("episode two", "episode 2")
                .replace("episode three", "episode 3")
                .replace("episode four", "episode 4")
                .replace("episode five", "episode 5")
                .replace("episode six", "episode 6")
                .replace("episode seven", "episode 7")
                .replace("episode eight", "episode 8")
                .replace("episode nine", "episode 9");
        converted = converted.replace("episode ix", "episode 9")
                .replace("episode viii", "episode 8")
                .replace("episode vii", "episode 7")
                .replace("episode vi", "episode 6")
                .replace("episode v", "episode 5")
                .replace("episode iv", "episode 4")
                .replace("episode iii", "episode 3")
                .replace("episode ii", "episode 2")
                .replace("episode i", "episode 1");
        return converted;
    }

    /** Lightweight roman numeral conversion for aliases like "Episode V". */
    private static String romanToArabic(String romanNumeral) {
        return switch (romanNumeral.toUpperCase(Locale.ROOT)) {
            case "I" -> "1";
            case "II" -> "2";
            case "III" -> "3";
            case "IV" -> "4";
            case "V" -> "5";
            case "VI" -> "6";
            case "VII" -> "7";
            case "VIII" -> "8";
            case "IX" -> "9";
            default -> romanNumeral;
        };
    }

    /** Calculates a normalised similarity score between zero and one using Levenshtein distance. */
    private static double similarity(String normalisedQuery, String normalisedCandidate) {
        if (normalisedCandidate.isEmpty()) {
            return 0.0;
        }
        if (normalisedCandidate.contains(normalisedQuery)) {
            return 1.0;
        }
        int distance = levenshtein(normalisedQuery, normalisedCandidate);
        int maxLength = Math.max(normalisedQuery.length(), normalisedCandidate.length());
        if (maxLength == 0) {
            return 0.0;
        }
        double ratio = 1.0 - ((double) distance / maxLength);
        // Boost partial token matches (e.g. "empire" inside the title) slightly.
        for (String token : normalisedQuery.split(" ")) {
            if (!token.isBlank() && normalisedCandidate.contains(token)) {
                ratio = Math.min(1.0, ratio + 0.08);
            }
        }
        return ratio;
    }

    /** Basic dynamic-programming Levenshtein distance implementation. */
    private static int levenshtein(String left, String right) {
        int[][] dp = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) {
            dp[i][0] = i;
        }
        for (int j = 0; j <= right.length(); j++) {
            dp[0][j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            char lc = left.charAt(i - 1);
            for (int j = 1; j <= right.length(); j++) {
                char rc = right.charAt(j - 1);
                int cost = lc == rc ? 0 : 1;
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + cost);
            }
        }
        return dp[left.length()][right.length()];
    }

    /** Adds a gentle weight to certain categories to stabilise tiebreakers. */
    private static double categoryBias(MovieCategory category) {
        return switch (category) {
            case SAGA -> 0.05;
            case ANTHOLOGY -> 0.03;
            default -> 0.0;
        };
    }

    /** Simple pair type capturing which field secured the best score. */
    private record MatchScore(String matchedField, double value) {
    }
}
