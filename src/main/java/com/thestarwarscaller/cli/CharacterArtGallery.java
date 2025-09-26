package com.thestarwarscaller.cli;

import java.util.List;
import java.util.Objects;

/**
 * Provides a curated list of ASCII-friendly portraits for beloved Star Wars characters.
 * Beginners can plug these lines straight into a CLI or web panel for a fun extra feature.
 */
public final class CharacterArtGallery {
    private CharacterArtGallery() {
    }

    /** @return all available portraits in a stable display order. */
    public static List<UnicodePortrait> portraits() {
        return PORTRAITS;
    }

    /**
     * Each portrait includes a name, a block of ASCII art lines, and some flavour text.
     * Feel free to add more characters as your collection grows.
     */
    private static final List<UnicodePortrait> PORTRAITS = List.of(
            new UnicodePortrait(
                    "Darth Vader",
                    List.of(
                            "    ########    ",
                            "  ### #### ###  ",
                            " ###  ##  ### ",
                            "###  ####  ###",
                            "###  ####  ###",
                            " ###      ### ",
                            "   ########   "
                    ),
                    List.of("Heavy breathing intensifies.")),
            new UnicodePortrait(
                    "Luke Skywalker",
                    List.of(
                            "    //////    ",
                            "   ////////   ",
                            "  /// ** ///  ",
                            " /// **** /// ",
                            "  /// ** ///  ",
                            "    //////    ",
                            "      **      "
                    ),
                    List.of("Bright-eyed farm boy turned Jedi.")),
            new UnicodePortrait(
                    "Princess Leia",
                    List.of(
                            "   ((((())))   ",
                            "  ((  **  ))  ",
                            " ((  ****  )) ",
                            " ((  ****  )) ",
                            "  (( **** ))  ",
                            "    ( ** )    ",
                            "     ****     "
                    ),
                    List.of("Hope has a hairstyle.")),
            new UnicodePortrait(
                    "Grogu",
                    List.of(
                            "  __////\\__  ",
                            " /  o  o  \\ ",
                            "|    ..    |",
                            "|  \\__/  |",
                            " \\  --  / ",
                            "  \\____/  ",
                            "     |||     "
                    ),
                    List.of("Snack seeker, Force wielder.")),
            new UnicodePortrait(
                    "Ahsoka Tano",
                    List.of(
                            "    /\\  /\\    ",
                            "   /  \\/  \\   ",
                            "  /  /\\  \\  ",
                            " /  /  \\  \\ ",
                            "  \\ \\__/ //  ",
                            "   \\____//   ",
                            "     ////     "
                    ),
                    List.of("This is the way of the Togruta.")),
            new UnicodePortrait(
                    "Din Djarin",
                    List.of(
                            "    ______    ",
                            "   / ____ \\   ",
                            "  / / __ \\ \\  ",
                            " / / |__| \\ \\ ",
                            " \\ \\ ____ / / ",
                            "  \\ \\____/ /  ",
                            "   \\______/   "
                    ),
                    List.of("Weapons are part of my religion.")),
            new UnicodePortrait(
                    "R2-D2",
                    List.of(
                            "    .----.    ",
                            "   / .--. \\   ",
                            "  | | oo | |  ",
                            "  | | == | |  ",
                            "  | |____| |  ",
                            "   \\______ /   ",
                            "    /_/\\_\\    "
                    ),
                    List.of("Beep boop translations not included.")),
            new UnicodePortrait(
                    "Chewbacca",
                    List.of(
                            "    //////    ",
                            "   ////////   ",
                            "  /// ** ///  ",
                            " /// **** /// ",
                            " /// **** /// ",
                            "  /// ** ///  ",
                            "    //////    "
                    ),
                    List.of("Let the Wookiee win."))
    );

    /**
     * Simple representation of a portrait consisting of lines and a signature.
     */
    public record UnicodePortrait(String name, List<String> faceLines, List<String> signatureLines) {
        public UnicodePortrait {
            Objects.requireNonNull(name, "name");
            Objects.requireNonNull(faceLines, "faceLines");
            Objects.requireNonNull(signatureLines, "signatureLines");
            if (faceLines.isEmpty()) {
                throw new IllegalArgumentException("faceLines must not be empty");
            }
            if (signatureLines.isEmpty()) {
                throw new IllegalArgumentException("signatureLines must not be empty");
            }
        }
    }
}
