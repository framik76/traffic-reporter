package net.manenti.reporter.report;

import net.manenti.reporter.model.IpStats;
import net.manenti.reporter.model.LogEntry;
import net.manenti.reporter.model.OutputFormat;
import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ReportGeneratorTest {

    private static final ReportGenerator generator = new ReportGenerator();

    private static LogEntry entry(String ip, long bytes) {
        return new LogEntry(LocalDateTime.now(), bytes, "OK", ip);
    }

    private static List<LogEntry> sampleEntries() {
        return Arrays.asList(
                entry("192.168.1.10", 1024),
                entry("192.168.1.10", 1500),
                entry("192.168.1.10",  800),
                entry("192.168.1.20", 2048),
                entry("192.168.1.20", 2200),
                entry("192.168.1.30", 4096)
        );
    }

    // =====================================================================
    //  ReportGenerator Aggregation Tests
    // =====================================================================

    @Test
    public void test_agg_sortedByRequestsDesc() {
        List<IpStats> stats = generator.aggregate(sampleEntries());
        boolean sorted = true;
        for (int i = 1; i < stats.size(); i++) {
            if (stats.get(i - 1).getRequestCount() < stats.get(i).getRequestCount()) {
                sorted = false;
                break;
            }
        }
        assertTrue("sorted DESC by requests", sorted);
    }

    @Test
    public void test_agg_firstIpHasMostRequests() {
        List<IpStats> stats = generator.aggregate(sampleEntries());
        assertEquals("top IP", "192.168.1.10", stats.get(0).getIpAddress());
        assertEquals("top count", 3, stats.get(0).getRequestCount());
    }

    @Test
    public void test_agg_bytesSum() {
        List<IpStats> stats = generator.aggregate(sampleEntries());
        IpStats ip10 = stats.stream()
                .filter(s -> s.getIpAddress().equals("192.168.1.10"))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals("bytes sum", 1024 + 1500 + 800, ip10.getTotalBytes());
    }

    @Test
    public void test_agg_requestPercentage() {
        List<IpStats> stats = generator.aggregate(sampleEntries());
        assertEquals("request pct 50%", 50.0, stats.get(0).getRequestPercentage(), 0.01);
    }

    @Test
    public void test_agg_emptyInput() {
        assertTrue("empty list", generator.aggregate(Collections.emptyList()).isEmpty());
    }

    @Test
    public void test_agg_nullInput() {
        assertTrue("null safe", generator.aggregate(null).isEmpty());
    }

    @Test
    public void test_agg_singleEntry100Pct() {
        List<IpStats> stats = generator.aggregate(Collections.singletonList(entry("10.0.0.1", 512)));
        assertEquals("100% requests", 100.0, stats.get(0).getRequestPercentage(), 0.01);
        assertEquals("100% bytes",    100.0, stats.get(0).getBytesPercentage(),   0.01);
    }

    // =====================================================================
    //  CSV Output Tests
    // =====================================================================

    @Test
    public void test_csv_headerPresent() throws IOException {
        Path out = Files.createTempFile("report", ".csv");
        generator.generate(sampleEntries(), out, OutputFormat.CSV);
        String content = new String(Files.readAllBytes(out), StandardCharsets.UTF_8);
        assertTrue("CSV header", content.startsWith("ip_address,request_count,request_pct,total_bytes,bytes_pct"));
        Files.deleteIfExists(out);
    }

    @Test
    public void test_csv_rowCount() throws IOException {
        Path out = Files.createTempFile("report", ".csv");
        generator.generate(sampleEntries(), out, OutputFormat.CSV);
        String[] lines = new String(Files.readAllBytes(out), StandardCharsets.UTF_8).trim().split("\\r?\\n");
        assertEquals("4 lines (1 header + 3 IPs)", 4, lines.length);
        Files.deleteIfExists(out);
    }

    @Test
    public void test_csv_firstDataRow() throws IOException {
        Path out = Files.createTempFile("report", ".csv");
        generator.generate(sampleEntries(), out, OutputFormat.CSV);
        String[] lines = new String(Files.readAllBytes(out), StandardCharsets.UTF_8).trim().split("\\r?\\n");
        assertTrue("first data row IP", lines[1].startsWith("192.168.1.10,3,"));
        Files.deleteIfExists(out);
    }

    @Test
    public void test_csv_emptyEntries() throws IOException {
        Path out = Files.createTempFile("empty", ".csv");
        generator.generate(Collections.emptyList(), out, OutputFormat.CSV);
        String[] lines = new String(Files.readAllBytes(out), StandardCharsets.UTF_8).trim().split("\\r?\\n");
        assertEquals("only header line", 1, lines.length);
        Files.deleteIfExists(out);
    }

    // =====================================================================
    //  JSON Output Tests
    // =====================================================================

    @Test
    public void test_json_validBrackets() throws IOException {
        Path out = Files.createTempFile("report", ".json");
        generator.generate(sampleEntries(), out, OutputFormat.JSON);
        String content = new String(Files.readAllBytes(out), StandardCharsets.UTF_8).trim();
        assertTrue("JSON starts [", content.startsWith("["));
        assertTrue("JSON ends ]",   content.endsWith("]"));
        Files.deleteIfExists(out);
    }

    @Test
    public void test_json_containsKeys() throws IOException {
        Path out = Files.createTempFile("report", ".json");
        generator.generate(sampleEntries(), out, OutputFormat.JSON);
        String content = new String(Files.readAllBytes(out), StandardCharsets.UTF_8);
        assertTrue("ip_address key",    content.contains("\"ip_address\""));
        assertTrue("request_count key", content.contains("\"request_count\""));
        assertTrue("total_bytes key",   content.contains("\"total_bytes\""));
        Files.deleteIfExists(out);
    }

    @Test
    public void test_json_emptyEntries() throws IOException {
        Path out = Files.createTempFile("empty", ".json");
        generator.generate(Collections.emptyList(), out, OutputFormat.JSON);
        String content = new String(Files.readAllBytes(out), StandardCharsets.UTF_8).trim();
        assertEquals("empty JSON array", "[]", content);
        Files.deleteIfExists(out);
    }

}