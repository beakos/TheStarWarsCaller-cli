package com.thestarwarscaller.core.search;

/**
 * Small utility record that bundles a search hit with its relevance score.
 * Keeping the score around lets the CLI decide how aggressively to surface suggestions.
 */
public record SearchResult<T>(T value, double score, String matchedField) {
}
