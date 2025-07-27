package crawler;

import model.CrawlConfig;
import model.SentenceMatch;
import parser.HTMLParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;

public class CrawlTask implements Runnable {
    private final String url;
    private final int depth;
    private final CrawlManager manager;
    private final CrawlConfig config;
    private final HTMLParser parser;
    private final Consumer<SentenceMatch> onMatch;

    public CrawlTask(String url, int depth, CrawlManager manager, CrawlConfig config, HTMLParser parser, Consumer<SentenceMatch> onMatch) {
        this.url = url;
        this.depth = depth;
        this.manager = manager;
        this.config = config;
        this.parser = parser;
        this.onMatch = onMatch;
    }

    @Override
    public void run() {
        try {
            Document doc = Jsoup.connect(url).timeout(5000).get();
            String html = doc.html();

            List<SentenceMatch> matchingSentences = parser.extractMatchingSentences(html, url);
            matchingSentences.forEach(sentenceMatch -> onMatch.accept(sentenceMatch));

            if (depth < this.config.getMaxDepth()) {
                Elements links = doc.select("a[href]");
                int followed = 0;

                for (Element link : links) {
                    final String absUrl = link.absUrl("href");
                    if (isInternalLink(absUrl) && !this.manager.hasVisited(absUrl)) {
                         this.manager.submitNewLink(absUrl, this.depth + 1);
                        if (++followed >= 10) break; // Cap at 10 links
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isInternalLink(String targetUrl) {
        try {
            URL base = new URL(this.config.getSeedUrl());
            URL target = new URL(targetUrl);
            return base.getHost().equalsIgnoreCase(target.getHost());
        } catch (MalformedURLException e) {
            return false;
        }
    }
}
