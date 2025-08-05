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
    private final AtomicInteger activeTasks;
    private final Consumer<SentenceMatch> onMatch;
    private final AtomicInteger pagesClaimed;
    private final Consumer<Integer> onProgressUpdate;

    public CrawlManager(CrawlConfig config, Consumer<SentenceMatch> onMatch, Consumer<Integer> onProgressUpdate) {
        this.config = config;
        this.parser = new HTMLParser(config.getTopic());
        this.executor = Executors.newFixedThreadPool(10);
        this.onMatch = onMatch;
        this.visited = ConcurrentHashMap.newKeySet();
        this.activeTasks = new AtomicInteger(0);
        this.pagesClaimed = new AtomicInteger(0);
        this.onProgressUpdate = onProgressUpdate;
    }

//    public boolean hasVisited(String url) {
//        return visited.contains(url);
//    }

    public void startCrawl() {
        final String seedUrl = this.config.getSeedUrl();
        this.visited.add(seedUrl);
        submitTask(seedUrl, 0);
        waitForAllTasks();
        this.executor.shutdown();
    }

    private void submitTask(String url, int depth) {
        if (depth > this.config.getMaxDepth()) return;

        this.activeTasks.incrementAndGet();

        this.executor.submit(() -> {
            try {
                CrawlTask task = new CrawlTask(url, depth, this, config, parser, onMatch);
                task.run();
            } finally {
                activeTasks.decrementAndGet();
            }
        });
    }

    public synchronized boolean submitNewLink(String url, int depth) {
        if (depth > this.config.getMaxDepth()) return false;

        if (this.visited.add(url)) {
            int count = this.pagesClaimed.incrementAndGet();
            if (count <= this.config.getMaxPages()) {
                onProgressUpdate.accept(count);
                submitTask(url, depth);

                return true;
            } else {
                this.visited.remove(url); // roll back both if over limit
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
