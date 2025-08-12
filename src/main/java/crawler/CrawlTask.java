package crawler;

import model.CrawlConfig;
import model.SentenceMatch;
import model.LinkRecord;
import parser.HTMLParser;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.*;
import java.util.List;
import java.util.function.Consumer;

public class CrawlTask implements Runnable {
    private final String url;         // normalized
    private final String parentUrl;   // normalized; null for seed
    private final int depth;

    private final CrawlManager manager;
    private final CrawlConfig config;
    private final HTMLParser parser;
    private final Consumer<SentenceMatch> onMatch;

    public CrawlTask(String url,
                     String parentUrl,
                     int depth,
                     CrawlManager manager,
                     CrawlConfig config,
                     HTMLParser parser,
                     Consumer<SentenceMatch> onMatch) {

        this.url = CrawlManager.normalizeUrl(url);
        this.parentUrl = parentUrl == null ? null : CrawlManager.normalizeUrl(parentUrl);
        this.depth = depth;
        this.manager = manager;
        this.config = config;
        this.parser = parser;
        this.onMatch = onMatch;
    }

    private static class HeadMeta {
        final int status;
        final long len;
        final String type;
        HeadMeta(int status, long len, String type) {
            this.status = status; this.len = len; this.type = type;
        }
    }

    // Quick HEAD to get status/length/content-type (no body)
    private static HeadMeta head(String urlStr) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setInstanceFollowRedirects(true);
            c.setRequestMethod("HEAD");
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            c.setRequestProperty("Accept-Encoding", "identity"); // real Content-Length
            c.connect();
            HeadMeta m = new HeadMeta(c.getResponseCode(), c.getContentLengthLong(), c.getContentType());
            c.disconnect();
            return m;
        } catch (Exception e) {
            return new HeadMeta(-1, -1, null);
        }
    }

    // Treat www and non-www as the same host (practical)
    private static String hostKey(String h) {
        if (h == null) return "";
        h = h.toLowerCase();
        return h.startsWith("www.") ? h.substring(4) : h;
    }

    // Generic rule: only http/https, same host as seed, skip obvious static files & non-page schemes
    private boolean shouldFollow(String targetUrl) {
        if (targetUrl == null) return false;
        String t = targetUrl.trim();
        if (t.isEmpty() || t.startsWith("javascript:") || t.startsWith("mailto:")) return false;

        try {
            URI base = new URI(this.config.getSeedUrl());
            URI u = new URI(t);

            String scheme = (u.getScheme() == null ? "http" : u.getScheme().toLowerCase());
            if (!scheme.equals("http") && !scheme.equals("https")) return false;

            if (!hostKey(u.getHost()).equals(hostKey(base.getHost()))) return false;

            String path = u.getPath() == null ? "" : u.getPath().toLowerCase();
            if (path.matches(".*\\.(?:jpg|jpeg|png|gif|webp|svg|ico|css|js|pdf|zip|gz|tar|rar|7z|mp3|mp4|avi|mov)$"))
                return false;

            return true;
        } catch (URISyntaxException e) {
            return false;
        }
    }

    @Override
    public void run() {
        HeadMeta meta = head(this.url);

        try {
            Document doc = Jsoup.connect(this.url).timeout(5000).get();
            String html = doc.html();

            List<SentenceMatch> matchingSentences = this.parser.extractMatchingSentences(html, url);
            matchingSentences.forEach(onMatch);

            if (this.depth < this.config.getMaxDepth()) {
                Elements links = doc.select("a[href]");
                int followed = 0;
                for (Element link : links) {
                    String absUrl = link.absUrl("href");
                    if (shouldFollow(absUrl)) {
                        String norm = CrawlManager.normalizeUrl(absUrl);
                        if (this.manager.submitNewLink(norm, this.url, this.depth + 1)) {
                            if (++followed >= 10) break; // limit per page (optional/configurable)
                        }
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error crawling " + this.url + ": " + e.getMessage());
        } finally {
            this.manager.log(new LinkRecord(
                    this.url,
                    this.parentUrl,
                    this.depth,
                    meta.status,
                    meta.len,
                    meta.type
            ));
        }
    }
}
