package com.thestarwarscaller.core;

/**
 * All the high-level categories used to group Star Wars films.
 * Imagine these like sections in the Jedi Archives where holovids are shelved.
 */
public enum MovieCategory {
    // The nine-part saga focused on the Skywalker bloodline.
    SAGA("Skywalker Saga"),
    // Standalone adventures such as Rogue One and Solo.
    ANTHOLOGY("Standalone Anthology Films"),
    // Feature-length animated releases like The Clone Wars movie.
    ANIMATED_FEATURE("Animated Features"),
    // Made-for-TV films including the Ewok duology and the Holiday Special.
    TV_FILM("Television Films and Specials"),
    // LEGO specials where minifigs wield the Force with detachable hands.
    LEGO_SPECIAL("LEGO and Animated Specials");

    /** Friendly label used in menus so users do not have to read all-caps names. */
    private final String displayName;

    MovieCategory(String displayName) {
        this.displayName = displayName;
    }

    /** @return the user-facing description of this category. */
    public String getDisplayName() {
        return displayName;
    }
}
