package net.manenti.reporter.parser;

import net.manenti.reporter.model.LogEntry;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Parses a semicolon-delimited log file into {@link LogEntry} objects,
 * silently skipping malformed lines and lines whose STATUS != "OK" (RFC 2616).
 *
 * Expected line format:
 *   TIMESTAMP;BYTES;STATUS;REMOTE_ADDR
 */
public class LogParser {

    private static final Logger LOG = Logger.getLogger(LogParser.class.getName());

    /** HTTP/1.1 "OK" status text (RFC 2616 §10.2.1). */
    static final String STATUS_OK = "OK";

    private static final String DELIMITER = ";";
    private static final int FIELD_COUNT  = 4;

    private static final int IDX_TIMESTAMP   = 0;
    private static final int IDX_BYTES       = 1;
    private static final int IDX_STATUS      = 2;
    private static final int IDX_REMOTE_ADDR = 3;

    // Support both date-only and date-time timestamps
    private static final DateTimeFormatter[] FORMATTERS = {
        DateTimeFormatter.ISO_LOCAL_DATE_TIME,          // 2024-01-15T08:00:01
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"),
        DateTimeFormatter.ofPattern("dd/MMM/yyyy:HH:mm:ss Z")
    };

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Parse all valid OK-status entries from the given file path.
     *
     * @param logPath path to the log file
     * @return list of valid {@link LogEntry} objects
     * @throws IOException if the file cannot be read
     */
    public List<LogEntry> parse(Path logPath) throws IOException {
        try (InputStream is = Files.newInputStream(logPath)) {
            return parse(is);
        }
    }

    /**
     * Parse all valid OK-status entries from an {@link InputStream}.
     * Useful for unit tests (pass a ByteArrayInputStream with test data).
     *
     * @param inputStream source data
     * @return list of valid {@link LogEntry} objects
     * @throws IOException if the stream cannot be read
     */
    public List<LogEntry> parse(InputStream inputStream) throws IOException {
        List<LogEntry> entries = new ArrayList<>();
        int lineNumber = 0;
        int skippedStatus = 0;
        int skippedMalformed = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                lineNumber++;
                line = line.trim();

                // skip blank lines and comment lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                String[] fields = line.split(DELIMITER, -1);
                if (fields.length != FIELD_COUNT) {
                    LOG.warning("Line " + lineNumber + ": expected " + FIELD_COUNT +
                                " fields, got " + fields.length + " — skipping.");
                    skippedMalformed++;
                    continue;
                }

                String status = fields[IDX_STATUS].trim();
                if (!STATUS_OK.equalsIgnoreCase(status)) {
                    skippedStatus++;
                    continue;
                }

                LogEntry entry = parseLine(lineNumber, fields);
                if (entry != null) {
                    entries.add(entry);
                } else {
                    skippedMalformed++;
                }
            }
        }

        LOG.info("Parsed " + entries.size() + " OK entries; " +
                 "skipped " + skippedStatus + " non-OK and " +
                 skippedMalformed + " malformed lines.");
        return entries;
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private LogEntry parseLine(int lineNumber, String[] fields) {
        String rawTimestamp = fields[IDX_TIMESTAMP].trim();
        String rawBytes     = fields[IDX_BYTES].trim();
        String rawAddr      = fields[IDX_REMOTE_ADDR].trim();

        LocalDateTime timestamp = parseTimestamp(rawTimestamp);
        if (timestamp == null) {
            LOG.warning("Line " + lineNumber + ": unparseable timestamp '" +
                        rawTimestamp + "' — skipping.");
            return null;
        }

        long bytes;
        try {
            bytes = Long.parseLong(rawBytes);
            if (bytes < 0) throw new NumberFormatException("negative");
        } catch (NumberFormatException e) {
            LOG.warning("Line " + lineNumber + ": invalid bytes value '" +
                        rawBytes + "' — skipping.");
            return null;
        }

        if (rawAddr.isEmpty()) {
            LOG.warning("Line " + lineNumber + ": empty REMOTE_ADDR — skipping.");
            return null;
        }

        return new LogEntry(timestamp, bytes, fields[IDX_STATUS].trim(), rawAddr);
    }

    private LocalDateTime parseTimestamp(String raw) {
        for (DateTimeFormatter fmt : FORMATTERS) {
            try {
                return LocalDateTime.parse(raw, fmt);
            } catch (DateTimeParseException ignored) {
                // try next formatter
            }
        }
        return null;
    }
}
