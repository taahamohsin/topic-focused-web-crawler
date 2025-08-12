package model;

public class LinkRecord {
    public final String url;
    public final String parentUrl;
    public final int depth;
    public final int status;
    public final long sizeBytes;
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

