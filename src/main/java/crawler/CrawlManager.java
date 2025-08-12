package crawler;

import model.SentenceMatch;
import model.CrawlConfig;
import model.LinkRecord;
import parser.HTMLParser;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CrawlManager {
    private final CrawlConfig config;
    private final HTMLParser parser;
    private final ExecutorService executor;
    private final Set<String> visited;
    private final AtomicInteger activeTasks;
    private final Consumer<SentenceMatch> onMatch;
    private final AtomicInteger pagesClaimed;
    private final Consumer<Integer> onProgressUpdate;
    private final List<LinkRecord> crawlLog;

    public CrawlManager(CrawlConfig config, Consumer<SentenceMatch> onMatch, Consumer<Integer> onProgressUpdate) {
        this.config = config;
        this.parser = new HTMLParser(config.getTopic());
        this.executor = Executors.newFixedThreadPool(10);
        this.onMatch = onMatch;
        this.visited = ConcurrentHashMap.newKeySet();
        this.activeTasks = new AtomicInteger(0);
        this.pagesClaimed = new AtomicInteger(0);
        this.onProgressUpdate = onProgressUpdate;
        this.crawlLog = Collections.synchronizedList(new ArrayList<>());
    }

    // Normalize URLs for dedupe/logging
    public static String normalizeUrl(String raw) {
        if (raw == null) return null;
        try {
            URI u = new URI(raw).normalize(); //
            String scheme = (u.getScheme() == null ? "http" : u.getScheme().toLowerCase());
            String host = (u.getHost() == null ? "" : u.getHost().toLowerCase());

            // drop default ports so the same page doesn't get treated as two different URLs
            int port = u.getPort();
            if ((scheme.equals("http") && port == 80) || (scheme.equals("https") && port == 443)) {
                port = -1;
            }

            String path = (u.getPath() == null || u.getPath().isEmpty()) ? "/" : u.getPath();
            if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);

            URI cleaned = new URI(scheme, u.getUserInfo(), host, port, path, u.getQuery(), null);
            return cleaned.toString();
        } catch (URISyntaxException e) {
            // fallback: strip fragment only
            int hash = raw.indexOf('#');
            return (hash >= 0) ? raw.substring(0, hash) : raw;
        }
    }

    public List<LinkRecord> getCrawlLog() {
        return this.crawlLog;
    }

    void log(LinkRecord r) {
        this.crawlLog.add(r);
    }

    public void startCrawl() {
        final String seedUrl = normalizeUrl(this.config.getSeedUrl());
        this.visited.add(seedUrl);

        submitTask(seedUrl, null, 0);
        waitForAllTasks();
        this.executor.shutdown();
    }

    /** Submit a task for a specific URL/parent/depth. */
    private void submitTask(String url, String parentUrl, int depth) {
        if (depth > this.config.getMaxDepth()) return;

        this.activeTasks.incrementAndGet();
        this.executor.submit(() -> {
            try {
                CrawlTask task = new CrawlTask(url, parentUrl, depth, this, config, parser, onMatch);
                task.run();
            } finally {
                activeTasks.decrementAndGet();
            }
        });
    }

    /**
     * Enqueues a newly discovered link and tracks
     * the parent URL for logging/ancestry and normalizes URLs for dedupe.
     */
    public synchronized boolean submitNewLink(String url, String parentUrl, int depth) {
        if (depth > this.config.getMaxDepth()) return false;

        String norm = normalizeUrl(url);
        if (this.visited.add(norm)) {
            int count = this.pagesClaimed.incrementAndGet();
            if (count <= this.config.getMaxPages()) {
                if (this.onProgressUpdate != null) {
                    onProgressUpdate.accept(count);
                }
                submitTask(norm, parentUrl == null ? null : normalizeUrl(parentUrl), depth);
                return true;
            } else {
                this.visited.remove(norm);
                this.pagesClaimed.decrementAndGet();
                return false;
            }
        }
        return false;
    }

    private void waitForAllTasks() {
        while (this.activeTasks.get() > 0) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
}
