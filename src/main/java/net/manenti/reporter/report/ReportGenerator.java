package net.manenti.reporter.report;

import net.manenti.reporter.model.IpStats;
import net.manenti.reporter.model.LogEntry;
import net.manenti.reporter.model.OutputFormat;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Aggregates {@link LogEntry} records by IP address and writes a daily report
 * in the requested format (CSV or JSON).
 *
 * <p>Aggregation rules:
 * <ul>
 *   <li>Count of requests per IP</li>
 *   <li>% of total requests</li>
 *   <li>Total bytes sent per IP</li>
 *   <li>% of total bytes</li>
 * </ul>
 * The output is sorted by request count descending.
 */
public class ReportGenerator {

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Aggregate entries and write the report to {@code outputPath}.
     *
     * @param entries    parsed log entries (only OK status)
     * @param outputPath destination file path (created or overwritten)
     * @param format     {@link OutputFormat#CSV} or {@link OutputFormat#JSON}
     * @throws IOException on any I/O failure
     */
    public void generate(List<LogEntry> entries, Path outputPath, OutputFormat format)
            throws IOException {

        List<IpStats> stats = aggregate(entries);

        Files.createDirectories(outputPath.getParent());

        try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
            switch (format) {
                case JSON: writeJson(stats, writer); break;
                case CSV:
                default:   writeCsv(stats, writer);  break;
            }
        }
    }

    /**
     * Aggregate log entries into per-IP statistics.
     * Public to allow direct unit testing.
     */
    public List<IpStats> aggregate(List<LogEntry> entries) {
        if (entries == null || entries.isEmpty()) {
            return Collections.emptyList();
        }

        // Accumulate per-IP counters
        Map<String, long[]> accumulator = new LinkedHashMap<>();
        for (LogEntry e : entries) {
            long[] counters = accumulator.computeIfAbsent(e.getRemoteAddr(), k -> new long[2]);
            counters[0]++;               // request count
            counters[1] += e.getBytes(); // total bytes
        }

        long totalRequests = entries.size();
        long totalBytes    = entries.stream().mapToLong(LogEntry::getBytes).sum();

        // Build IpStats list
        List<IpStats> stats = new ArrayList<>(accumulator.size());
        for (Map.Entry<String, long[]> entry : accumulator.entrySet()) {
            long reqCount  = entry.getValue()[0];
            long byteCount = entry.getValue()[1];

            stats.add(new IpStats(
                entry.getKey(),
                reqCount,
                percentage(reqCount, totalRequests),
                byteCount,
                percentage(byteCount, totalBytes)
            ));
        }

        // Sort by request count descending; tie-break by IP for determinism
        stats.sort(Comparator
            .comparingLong(IpStats::getRequestCount).reversed()
            .thenComparing(IpStats::getIpAddress));

        return stats;
    }

    // -------------------------------------------------------------------------
    // CSV writer
    // -------------------------------------------------------------------------

    private void writeCsv(List<IpStats> stats, BufferedWriter writer) throws IOException {
        writer.write("ip_address,request_count,request_pct,total_bytes,bytes_pct");
        writer.newLine();

        for (IpStats s : stats) {
            writer.write(String.join(",",
                escapeCsv(s.getIpAddress()),
                Long.toString(s.getRequestCount()),
                formatPct(s.getRequestPercentage()),
                Long.toString(s.getTotalBytes()),
                formatPct(s.getBytesPercentage())
            ));
            writer.newLine();
        }
    }

    /** Wrap a CSV field in double-quotes if it contains a comma, quote, or newline. */
    private String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // JSON writer (hand-rolled; no external libraries required)
    // -------------------------------------------------------------------------

    private void writeJson(List<IpStats> stats, BufferedWriter writer) throws IOException {
        if (stats.isEmpty()) {
            writer.write("[]");
            writer.newLine();
            return;
        }
        writer.write("[");
        writer.newLine();

        for (int i = 0; i < stats.size(); i++) {
            IpStats s = stats.get(i);
            writer.write("  {");
            writer.newLine();
            writer.write("    \"ip_address\": "      + jsonString(s.getIpAddress())            + ","); writer.newLine();
            writer.write("    \"request_count\": "   + s.getRequestCount()                      + ","); writer.newLine();
            writer.write("    \"request_pct\": "     + formatPct(s.getRequestPercentage())      + ","); writer.newLine();
            writer.write("    \"total_bytes\": "     + s.getTotalBytes()                        + ","); writer.newLine();
            writer.write("    \"bytes_pct\": "       + formatPct(s.getBytesPercentage()));
            writer.newLine();
            writer.write("  }" + (i < stats.size() - 1 ? "," : ""));
            writer.newLine();
        }

        writer.write("]");
        writer.newLine();
    }

    private String jsonString(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private double percentage(long part, long total) {
        return total == 0 ? 0.0 : (part * 100.0) / total;
    }

    private String formatPct(double pct) {
        return String.format(Locale.US, "%.2f", pct);
    }
}
