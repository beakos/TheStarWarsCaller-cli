package com.thestarwarscaller.core;

import java.util.Locale;
import java.util.Objects;

/**
 * Represents any Star Wars television or streaming series.
 * In lore terms, this is the datapad entry for every show on the holonet.
 */
public final class Series {
    /** Title of the show (for example "The Mandalorian"). */
    private final String title;
    /** First year the show aired. */
    private final int startYear;
    /** Last year of release (null means still ongoing like Grogu's adventures). */
    private final Integer endYear;
    /** Whether the show is live action, animated, documentary, etc. */
    private final SeriesFormat format;
    /** Which era of the Star Wars timeline this series explores. */
    private final String era;
    /** Short description so newcomers know why to binge it. */
    private final String synopsis;
    /** Streaming platform or release channel. */
    private final String streaming;
    /** Bonus trivia such as creators or production notes. */
    private final String notes;

    /**
     * Constructor that wires all of the above into an immutable bundle.
     * Imagine stamping this onto a Holonet archive card.
     */
    public Series(
            String title,
            int startYear,
            Integer endYear,
            SeriesFormat format,
            String era,
            String synopsis,
            String streaming,
            String notes) {
        this.title = Objects.requireNonNull(title, "title");
        this.startYear = startYear;
        this.endYear = endYear;
        this.format = Objects.requireNonNull(format, "format");
        this.era = era;
        this.synopsis = synopsis;
        this.streaming = streaming;
        this.notes = notes;
    }

    public String getTitle() {
        return title;
    }

    public int getStartYear() {
        return startYear;
    }

    public Integer getEndYear() {
        return endYear;
    }

    public SeriesFormat getFormat() {
        return format;
    }

    public String getEra() {
        return era;
    }

    public String getSynopsis() {
        return synopsis;
    }

    public String getStreaming() {
        return streaming;
    }

    public String getNotes() {
        return notes;
    }

    /**
     * True if this series'' text contains the query text anywhere useful.
     * Lower-casing ensures even Imperial droids with caps lock stuck on can search.
     */
    public boolean matches(String query) {
        String lower = query.toLowerCase(Locale.ROOT);
        if (title.toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        if (era != null && era.toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        if (synopsis != null && synopsis.toLowerCase(Locale.ROOT).contains(lower)) {
            return true;
        }
        return notes != null && notes.toLowerCase(Locale.ROOT).contains(lower);
    }

    /**
     * Formats a multi-line description ideal for the CLI output.
     * Think of the result as a mission briefing from General Dodonna.
     */
    public String describe() {
        StringBuilder sb = new StringBuilder();
        sb.append(title);
        sb.append(" (");
        sb.append(startYear);
        if (endYear != null) {
            sb.append('-');
            sb.append(endYear);
        } else {
            sb.append("-present");
        }
        sb.append(") [");
        sb.append(format.getDisplayName());
        sb.append(']');
        if (era != null && !era.isBlank()) {
            sb.append(" | Era: ");
            sb.append(era);
        }
        if (streaming != null && !streaming.isBlank()) {
            sb.append(" | Watch: ");
            sb.append(streaming);
        }
        if (synopsis != null && !synopsis.isBlank()) {
            sb.append("\n    ");
            sb.append(synopsis);
        }
        if (notes != null && !notes.isBlank()) {
            sb.append("\n    Notes: ");
            sb.append(notes);
        }
        return sb.toString();
    }
}
