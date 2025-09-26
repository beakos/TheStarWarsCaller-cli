package com.thestarwarscaller.core;

/**
 * Lists the different formats our Star Wars television entries can have.
 * Picture each value as a tag on a holonet broadcast listing.
 */
public enum SeriesFormat {
    // Classic live-action shows like The Mandalorian or Andor.
    LIVE_ACTION("Live Action Series"),
    // Fully animated adventures from Lucasfilm Animation.
    ANIMATED("Animated Series"),
    // Short-run micro series such as Tartakovsky''s Clone Wars.
    MICRO_SERIES("Animated Micro Series"),
    // LEGO shows filled with brick-built hilarity.
    LEGO("LEGO Animated Series"),
    // Anthology series where each episode tells a unique tale.
    ANTHOLOGY("Anthology Series"),
    // Documentary content exploring the behind-the-scenes magic.
    DOCUMENTARY("Documentary Series");

    /** Human-friendly label for UI display. */
    private final String displayName;

    SeriesFormat(String displayName) {
        this.displayName = displayName;
    }

    /** @return the nice readable name for this format. */
    public String getDisplayName() {
        return displayName;
    }
}
