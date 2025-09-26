package com.thestarwarscaller.core;

import java.util.Collection;

/**
 * Utility focused on converting our media data back into JSON text.
 * Consider this the protocol droid that translates our catalog for other apps.
 */
public final class JsonExporter {
    private JsonExporter() {
        // No instances needed; the Force flows through static methods only.
    }

    /**
     * Serialises a collection of {@link Movie} objects into JSON.
     * Useful when the CLI exports data or when another app wants to import our list.
     */
    public static String moviesToJson(Collection<Movie> movies) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"movies\": [\n");
        appendMovieEntries(movies, sb);
        sb.append("  ]\n}");
        return sb.toString();
    }

    /** Serialises a collection of {@link Series} objects into JSON. */
    public static String seriesToJson(Collection<Series> series) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\n  \"series\": [\n");
        appendSeriesEntries(series, sb);
        sb.append("  ]\n}");
        return sb.toString();
    }

    /** Writes each movie as an object in the JSON array. */
    private static void appendMovieEntries(Collection<Movie> movies, StringBuilder sb) {
        boolean first = true;
        for (Movie movie : movies) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append("    {");
            sb.append("\n      \"title\": \"").append(escape(movie.getTitle())).append("\",");
            if (movie.getEpisodeNumber() != null) {
                sb.append("\n      \"episodeNumber\": ").append(movie.getEpisodeNumber()).append(',');
            }
            if (movie.getEpisodeRoman() != null) {
                sb.append("\n      \"episodeRoman\": \"").append(escape(movie.getEpisodeRoman())).append("\",");
            }
            sb.append("\n      \"releaseYear\": ").append(movie.getReleaseYear()).append(',');
            sb.append("\n      \"category\": \"").append(movie.getCategory().name()).append("\",");
            if (movie.getEra() != null) {
                sb.append("\n      \"era\": \"").append(escape(movie.getEra())).append("\",");
            }
            if (movie.getSynopsis() != null) {
                sb.append("\n      \"synopsis\": \"").append(escape(movie.getSynopsis())).append("\",");
            }
            if (movie.getStreaming() != null) {
                sb.append("\n      \"streaming\": \"").append(escape(movie.getStreaming())).append("\",");
            }
            if (movie.getNotes() != null) {
                sb.append("\n      \"notes\": \"").append(escape(movie.getNotes())).append("\",");
            }
            trimTrailingComma(sb);
            sb.append("\n    }");
        }
        sb.append('\n');
    }

    /** Writes each series entry as an object in the JSON array. */
    private static void appendSeriesEntries(Collection<Series> series, StringBuilder sb) {
        boolean first = true;
        for (Series show : series) {
            if (!first) {
                sb.append(",\n");
            }
            first = false;
            sb.append("    {");
            sb.append("\n      \"title\": \"").append(escape(show.getTitle())).append("\",");
            sb.append("\n      \"startYear\": ").append(show.getStartYear()).append(',');
            if (show.getEndYear() != null) {
                sb.append("\n      \"endYear\": ").append(show.getEndYear()).append(',');
            }
            sb.append("\n      \"format\": \"").append(show.getFormat().name()).append("\",");
            if (show.getEra() != null) {
                sb.append("\n      \"era\": \"").append(escape(show.getEra())).append("\",");
            }
            if (show.getSynopsis() != null) {
                sb.append("\n      \"synopsis\": \"").append(escape(show.getSynopsis())).append("\",");
            }
            if (show.getStreaming() != null) {
                sb.append("\n      \"streaming\": \"").append(escape(show.getStreaming())).append("\",");
            }
            if (show.getNotes() != null) {
                sb.append("\n      \"notes\": \"").append(escape(show.getNotes())).append("\",");
            }
            trimTrailingComma(sb);
            sb.append("\n    }");
        }
        sb.append('\n');
    }

    /** Escapes characters that would break JSON formatting. */
    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** Removes trailing commas from the current JSON object. */
    private static void trimTrailingComma(StringBuilder sb) {
        int length = sb.length();
        for (int i = length - 1; i >= 0; i--) {
            char c = sb.charAt(i);
            if (Character.isWhitespace(c)) {
                continue;
            }
            if (c == ',') {
                sb.deleteCharAt(i);
            }
            break;
        }
        // Easter egg: this method saves the day more often than Han in the Falcon.
    }
}
