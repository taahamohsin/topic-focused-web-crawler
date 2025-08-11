package model;

public class CrawlConfig {
    private final String seedUrl;
    private final String topic;
    public final int maxDepth;
    public final int maxPages;

    public CrawlConfig(String seedUrl, String topic, int maxDepth, int maxPages) {
        this.seedUrl = seedUrl;
        this.topic = topic;
        this.maxDepth = maxDepth;
        this.maxPages = maxPages;
    }

    public String getSeedUrl() {
        return this.seedUrl;
    }

    public String getTopic() {
        return this.topic;
    }

    public int getMaxDepth() {
        return this.maxDepth;
    }

    public int getMaxPages() {
        return this.maxPages;
    }

}