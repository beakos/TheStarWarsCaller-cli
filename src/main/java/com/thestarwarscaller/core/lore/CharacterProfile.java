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
        this.aliases = aliases == null ? List.of() : List.copyOf(aliases);
        this.affiliations = affiliations == null ? List.of() : List.copyOf(affiliations);
        this.mediaAppearances = mediaAppearances == null ? List.of() : List.copyOf(mediaAppearances);
    }
}
