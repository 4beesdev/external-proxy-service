package externalproxy.support.exception;

public class TotalMediaSizeExceededException extends RuntimeException {
    private final long limitBytes;
    private final long totalBytes;

    public TotalMediaSizeExceededException(long totalBytes, long limitBytes) {
        super("Total media size exceeds limit: " + totalBytes + " > " + limitBytes);
        this.limitBytes = limitBytes;
        this.totalBytes = totalBytes;
    }

    public long getLimitBytes() {
        return limitBytes;
    }

    public long getTotalBytes() {
        return totalBytes;
    }
}

