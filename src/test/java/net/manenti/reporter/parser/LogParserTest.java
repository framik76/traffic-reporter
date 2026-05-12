package net.manenti.reporter.parser;

import net.manenti.reporter.model.LogEntry;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class LogParserTest  {

    private static final LogParser parser = new LogParser();

    @Test
    public void test_parser_parsesValidOkEntry() throws IOException {
        var logContent = "2024-01-15T08:00:01;1024;OK;192.168.1.10";
        InputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = parser.parse(is);
        assertEquals("entry count", 1, entries.size());
        assertEquals("remote addr", "192.168.1.10", entries.get(0).getRemoteAddr());
        assertEquals("bytes",       1024L, entries.get(0).getBytes());
    }

    @Test
    public void test_parser_filtersNonOkStatus() throws IOException {
        var logContent =
                "2024-01-15T08:00:01;1024;OK;192.168.1.10\n" +
                "2024-01-15T08:00:02;2048;NOT FOUND;192.168.1.20\n" +
                "2024-01-15T08:00:03;512;FORBIDDEN;192.168.1.30";
        InputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = parser.parse(is);
        assertEquals("only OK entries", 1, entries.size());
        assertEquals("correct ip", "192.168.1.10", entries.get(0).getRemoteAddr());
    }

    @Test
    public void test_parser_skipsMalformedLines() throws IOException {
        var logContent =
                "2024-01-15T08:00:01;1024;OK;192.168.1.10\n" +
                "this-is-garbage\n" +
                "2024-01-15T08:00:02;800;OK;192.168.1.20";
        InputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = parser.parse(is);
        assertEquals("two valid entries", 2, entries.size());
    }

    @Test
    public void test_parser_skipsBlankLines() throws IOException {
        var logContent = "\n2024-01-15T08:00:01;1024;OK;192.168.1.10\n\n";
        InputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        assertEquals("one entry", 1, parser.parse(is).size());
    }

    @Test
    public void test_parser_emptyLogReturnsEmpty() throws IOException {
        InputStream is = new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8));
        assertTrue("empty list", parser.parse(is).isEmpty());
    }

    @Test
    public void test_parser_skipsNegativeBytes() throws IOException {
        var logContent =
                "2024-01-15T08:00:01;-100;OK;192.168.1.10\n" +
                "2024-01-15T08:00:02;1024;OK;192.168.1.20";
        InputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        List<LogEntry> entries = parser.parse(is);
        assertEquals("only valid entry", 1, entries.size());
        assertEquals("correct ip", "192.168.1.20", entries.get(0).getRemoteAddr());
    }

    @Test
    public void test_parser_skipsBadBytesField() throws IOException {
        var logContent =
                "2024-01-15T08:00:01;notanumber;OK;192.168.1.10\n" +
                "2024-01-15T08:00:02;2048;OK;192.168.1.20";
        InputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        assertEquals("only valid entry", 1, parser.parse(is).size());
    }

    @Test
    public void test_parser_caseInsensitiveOk() throws IOException {
        var logContent = "2024-01-15T08:00:01;1024;ok;192.168.1.10";
        InputStream is = new ByteArrayInputStream(logContent.getBytes(StandardCharsets.UTF_8));
        assertEquals("ok lowercased accepted", 1, parser.parse(is).size());
    }

}