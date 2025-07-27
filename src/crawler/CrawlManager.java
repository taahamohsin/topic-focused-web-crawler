package crawler;

import model.SentenceMatch;
import model.CrawlConfig;
import parser.HTMLParser;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class CrawlManager {
    private final CrawlConfig config;
    private final HTMLParser parser;
    private final ExecutorService executor;
    private final Set<String> visited;
    private final Queue<Future<?>> futures;
    private final Consumer<SentenceMatch> onMatch;
    private final AtomicInteger pagesClaimed = new AtomicInteger(0);


    public CrawlManager(CrawlConfig config, Consumer<SentenceMatch> onMatch) {
        this.config = config;
        String topic = config.getTopic();
        this.parser = new HTMLParser(topic);
        this.executor = Executors.newFixedThreadPool(10);
        this.onMatch = onMatch;
        this.visited = ConcurrentHashMap.newKeySet();
        this.futures = new ConcurrentLinkedQueue<>();
    }

    public boolean hasVisited(String url) {
        return this.visited.contains(url);
    }

    public void startCrawl() {
        final String seedUrl = config.getSeedUrl();
        visited.add(seedUrl);
        submitTask(seedUrl, 0);
        waitForAllTasks();
        executor.shutdown();
    }

    private void submitTask(String url, int depth) {
        if (visited.size() >= config.getMaxPages() || depth > config.getMaxDepth()) return;

        CrawlTask task = new CrawlTask(url, depth, this, this.config, this.parser, this.onMatch);
        this.futures.add(this.executor.submit(task));
    }

    public void submitNewLink(String url, int depth) {
        if (depth > config.getMaxDepth()) return;

        if (visited.add(url)) {
            int count = this.pagesClaimed.incrementAndGet();
            if (count >= config.getMaxPages()) {
                submitTask(url, depth);
            } else {
                visited.remove(url);
            }
        }
    }

    private void waitForAllTasks() {
        for (Future<?> future : this.futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("Error waiting for crawl task: " + e.getMessage());

            }
        }
    }
}
