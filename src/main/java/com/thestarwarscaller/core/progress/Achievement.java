package com.thestarwarscaller.core.progress;

/**
 * Achievements represent long-term goals surfaced to the user as they log more watch history.
 */
public enum Achievement {
    SAGA_COMPLETE("Watch every film in the Skywalker Saga"),
    ANTHOLOGY_EXPLORER("Complete all anthology adventures"),
    LEGO_MASTER_BUILDER("Finish every LEGO special"),
    CLONE_WARS_COMPLETIONIST("Experience the entire Clone Wars saga");

    private final String description;

    Achievement(String description) {
        this.description = description;
    }

    /** @return friendly string used in menus and progress readouts. */
    public String description() {
        return description;
    }
}
