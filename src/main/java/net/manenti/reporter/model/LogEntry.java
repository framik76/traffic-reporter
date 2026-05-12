package net.manenti.reporter.model;

import java.time.LocalDateTime;

/**
 * Represents a single parsed record from requests.log.
 * Format: TIMESTAMP;BYTES;STATUS;REMOTE_ADDR
 */
public class LogEntry {

    private final LocalDateTime timestamp;
    private final long bytes;
    private final String status;
    private final String remoteAddr;

    public LogEntry(LocalDateTime timestamp, long bytes, String status, String remoteAddr) {
        this.timestamp = timestamp;
        this.bytes = bytes;
        this.status = status;
        this.remoteAddr = remoteAddr;
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public long getBytes()              { return bytes; }
    public String getStatus()           { return status; }
    public String getRemoteAddr()       { return remoteAddr; }

    @Override
    public String toString() {
        return "LogEntry{timestamp=" + timestamp +
               ", bytes=" + bytes +
               ", status='" + status + '\'' +
               ", remoteAddr='" + remoteAddr + '\'' + '}';
    }
}
