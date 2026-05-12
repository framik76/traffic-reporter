package net.manenti.reporter.model;

/**
 * Aggregated traffic statistics for a single IP address.
 */
public class IpStats {

    private final String ipAddress;
    private final long requestCount;
    private final double requestPercentage;
    private final long totalBytes;
    private final double bytesPercentage;

    public IpStats(String ipAddress,
                   long requestCount,
                   double requestPercentage,
                   long totalBytes,
                   double bytesPercentage) {
        this.ipAddress         = ipAddress;
        this.requestCount      = requestCount;
        this.requestPercentage = requestPercentage;
        this.totalBytes        = totalBytes;
        this.bytesPercentage   = bytesPercentage;
    }

    public String getIpAddress()        { return ipAddress; }
    public long   getRequestCount()     { return requestCount; }
    public double getRequestPercentage(){ return requestPercentage; }
    public long   getTotalBytes()       { return totalBytes; }
    public double getBytesPercentage()  { return bytesPercentage; }

    @Override
    public String toString() {
        return "IpStats{ip='" + ipAddress + '\'' +
               ", requests=" + requestCount +
               ", reqPct=" + requestPercentage +
               ", bytes=" + totalBytes +
               ", bytesPct=" + bytesPercentage + '}';
    }
}
