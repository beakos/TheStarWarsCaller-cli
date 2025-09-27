package com.thestarwarscaller.core.lore;

import java.util.List;
import java.util.Objects;

/** Describes a character and where they appear across canon. */
public record CharacterProfile(
        String name,
        List<String> aliases,
        List<String> affiliations,
        String homeworld,
        String biography,
        List<String> mediaAppearances) {

    public CharacterProfile {
        Objects.requireNonNull(name, "name");
        aliases = sanitize(aliases);
        affiliations = sanitize(affiliations);
        mediaAppearances = sanitize(mediaAppearances);
    }

    private static List<String> sanitize(List<String> values) {
        return values == null ? List.of() : List.copyOf(values);
    }
}
