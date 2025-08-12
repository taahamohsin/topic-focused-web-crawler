package model;

public class LinkRecord {
    public final String url;
    public final String parentUrl; // null for seed
    public final int depth;
    public final int status;       // HTTP status or -1 on error
    public final long sizeBytes;   // Content-Length or measured bytes; -1 if unknown
    public final String contentType;

    public LinkRecord(String url, String parentUrl, int depth, int status, long sizeBytes, String contentType) {
        this.url = url;
        this.parentUrl = parentUrl;
        this.depth = depth;
        this.status = status;
        this.sizeBytes = sizeBytes;
        this.contentType = contentType;
    }
}

