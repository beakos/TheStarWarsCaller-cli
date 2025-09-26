package com.thestarwarscaller.core;

import java.nio.file.Path;

/**
 * Tiny smoke test to be sure the JSON loader understands our data files.
 * Not as flashy as a trench run, but still saves the day when something breaks.
 */
public final class MediaRepositoryTest {
    public static void main(String[] args) throws Exception {
        MediaCatalog catalog = MediaRepository.loadCatalog(
                Path.of("data", "movies.json"),
                Path.of("data", "series.json"));

        assertEquals("movie count", 18, catalog.getMovies().size());
        assertEquals("series count", 26, catalog.getSeries().size());

        Movie rogueOne = catalog.searchMovies("Rogue One").stream().findFirst()
                .orElseThrow(() -> new AssertionError("Rogue One should be present"));
        assertEquals("rogue one year", 2016, rogueOne.getReleaseYear());

        Series mandalorian = catalog.searchSeries("Mandalorian").stream().findFirst()
                .orElseThrow(() -> new AssertionError("Mandalorian should be present"));
        assertEquals("mandalorian year", 2019, mandalorian.getStartYear());

        System.out.println("All MediaRepository tests passed.");
    }

    /** Simple assertion helper so we do not pull in a test framework yet. */
    private static void assertEquals(String label, Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError(label + " expected=" + expected + " actual=" + actual);
        }
        // Easter egg: if this fails, imagine Obi-Wan whispering "Use the debugger".
    }
}

