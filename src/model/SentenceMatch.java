package model;

public class SentenceMatch {
    public final String sentence;
    public final String sourceUrl;

    public SentenceMatch(String sentence, String sourceUrl) {
        this.sentence = sentence;
        this.sourceUrl = sourceUrl;
    }

    public String getSentence() {
        return this.sentence;
    }

    public String getSourceUrl() {
        return this.sourceUrl;
    }
}