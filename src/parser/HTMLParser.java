package parser;

import model.SentenceMatch;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class HTMLParser {
    private final String query;

    public HTMLParser(String query) {
        this.query = query;
    }

    public List<SentenceMatch> extractMatchingSentences(String html, String url) {
        Document doc = Jsoup.parse(html);
        String text = doc.body().text();

        String[] sentences = text.split("(?<=[.!?])\\s+");
        List<SentenceMatch> matches = new ArrayList<>();

        for (String sentence : sentences) {
            String regex = "\\b" + Pattern.quote(this.query.toLowerCase()) + "(s|ing|ed)?\\b";
            Pattern pattern = Pattern.compile(regex);

            if (pattern.matcher(sentence.toLowerCase()).find()) {
                matches.add(new SentenceMatch(sentence.trim(), url));
            }
        }

        return matches;
    }
}