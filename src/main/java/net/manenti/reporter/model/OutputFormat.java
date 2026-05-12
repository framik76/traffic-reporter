package net.manenti.reporter.model;

/**
 * Supported output formats for the daily report.
 */
public enum OutputFormat {
    CSV,
    JSON;

    /**
     * Parse a format name case-insensitively, defaulting to CSV.
     */
    public static OutputFormat fromString(String value) {
        if (value == null) return CSV;
        switch (value.trim().toUpperCase()) {
            case "JSON": return JSON;
            default:     return CSV;
        }
    }
}
