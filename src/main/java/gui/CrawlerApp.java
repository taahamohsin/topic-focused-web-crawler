package gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import model.CrawlConfig;
import crawler.CrawlManager;

public class CrawlerApp extends JFrame {
    private final JTextField urlField;
    private final JTextField topicField;
    private final JTextField depthField;
    private final JTextField maxPagesField;
    private final JButton startButton;
    private final JTextPane resultsArea;
    private final JLabel statusLabel;
    private final AtomicInteger resultCount = new AtomicInteger(0);
    private final StyledDocument documentReference;
    private final JPanel searchPanel;
    private final JTextField searchField;
    private final JButton searchButton;
    private final JButton findNextButton;
    private final Highlighter highlighter;
    private final Highlighter.HighlightPainter highlightPainter;
    private int lastSearchIndex = 0;
    private String lastSearchTerm = "";
    private final JPanel centerPanel;
    private final Style defaultStyle;
    private long crawlStartTime;

    // Keep a handle to the scroll pane and a simple loading panel shown IN the results area
    private final JScrollPane resultsScroll;
    private final JPanel loadingPanel;
    private boolean resultsShownOnce = false;

    public CrawlerApp() {
        super("Topic-Focused Web Crawler");

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);

        // Top panel for inputs
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 10, 10));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Crawl Settings"));

        urlField = new JTextField("https://example.com");
        topicField = new JTextField("example");
        depthField = new JTextField("1");
        maxPagesField = new JTextField("10");
        statusLabel = new JLabel();

        inputPanel.add(new JLabel("Seed URL:"));
        inputPanel.add(urlField);
        inputPanel.add(new JLabel("Keyword:"));
        inputPanel.add(topicField);
        inputPanel.add(new JLabel("Depth Limit:"));
        inputPanel.add(depthField);
        inputPanel.add(new JLabel("Max Pages:"));
        inputPanel.add(maxPagesField);

        startButton = new JButton("Start Crawl");
        startButton.addActionListener(this::handleStart);

        // Results area + document
        resultsArea = new JTextPane();
        documentReference = resultsArea.getStyledDocument();
        defaultStyle = resultsArea.addStyle("DefaultStyle", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);
        resultsArea.setEditable(false);

        resultsScroll = new JScrollPane(resultsArea);
        resultsScroll.setBorder(BorderFactory.createTitledBorder("Crawl Results"));

        // Loading panel shown in place of resultsArea inside the scroll pane's viewport
        loadingPanel = new JPanel(new GridBagLayout());
        JLabel loadingLabel = new JLabel("<html><div style='text-align:center;'>⏳<br/>Currently crawling…<br/></html>");
        loadingLabel.setHorizontalAlignment(SwingConstants.CENTER);
        loadingPanel.add(loadingLabel);

        // Search panel
        searchField = new JTextField(20);
        searchButton = new JButton("Find");
        findNextButton = new JButton("Find Next");
        findNextButton.setEnabled(false);

        searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        searchPanel.add(new JLabel("Search:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(findNextButton);
        searchPanel.setVisible(false);

        this.highlighter = resultsArea.getHighlighter();
        this.highlightPainter = new DefaultHighlighter.DefaultHighlightPainter(Color.YELLOW);

        searchButton.addActionListener(e -> {
            lastSearchTerm = searchField.getText().trim();
            if (lastSearchTerm.isEmpty()) return;
            highlighter.removeAllHighlights();
            lastSearchIndex = 0;
            findNextButton.setEnabled(true);
            findNextMatch();
        });

        findNextButton.addActionListener(e -> {
            if (lastSearchTerm.isEmpty()) lastSearchTerm = searchField.getText().trim();
            findNextMatch();
        });

        // Center panel (kept as BorderLayout so we can drop searchPanel NORTH later)
        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(resultsScroll, BorderLayout.CENTER);

        // Top container for inputs and start button
        JPanel top = new JPanel(new BorderLayout());
        top.add(inputPanel, BorderLayout.CENTER);
        JPanel bottomOfTop = new JPanel(new BorderLayout());
        top.add(bottomOfTop, BorderLayout.SOUTH);
        bottomOfTop.add(startButton, BorderLayout.NORTH);

        add(top, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void findNextMatch() {
        String content = resultsArea.getText().toLowerCase();
        String searchTerm = lastSearchTerm.toLowerCase();

        int index = content.indexOf(searchTerm, lastSearchIndex);
        if (index == -1 && lastSearchIndex > 0) index = content.indexOf(searchTerm);

        if (index >= 0) {
            try {
                int end = index + searchTerm.length();
                highlighter.removeAllHighlights();
                highlighter.addHighlight(index, end, highlightPainter);
                resultsArea.setCaretPosition(end);
                lastSearchIndex = end;
            } catch (BadLocationException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void appendWithHighlight(String sentence, String keyword) {
        Style keywordStyle = resultsArea.getStyle("KeywordStyle");
        if (keywordStyle == null) {
            keywordStyle = resultsArea.addStyle("KeywordStyle", null);
            StyleConstants.setForeground(keywordStyle, Color.GREEN);
            StyleConstants.setBold(keywordStyle, true);
        }

        int lastIndex = 0;
        String lowerSentence = sentence.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        while (lastIndex < sentence.length()) {
            int index = lowerSentence.indexOf(lowerKeyword, lastIndex);
            if (index == -1) {
                try {
                    documentReference.insertString(documentReference.getLength(), sentence.substring(lastIndex), defaultStyle);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                documentReference.insertString(documentReference.getLength(), sentence.substring(lastIndex, index), defaultStyle);
                documentReference.insertString(documentReference.getLength(), sentence.substring(index, index + keyword.length()), keywordStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            lastIndex = index + keyword.length();
        }

        try {
            documentReference.insertString(documentReference.getLength(), "\n\n", defaultStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void clearResults() {
        try {
            documentReference.remove(0, documentReference.getLength());
            documentReference.insertString(0, "Starting crawl...\n\n", defaultStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void handleStart(ActionEvent e) {
        String url = urlField.getText().trim();
        String topic = topicField.getText().trim();
        int depth = Integer.parseInt(depthField.getText().trim());
        int maxPages = Integer.parseInt(maxPagesField.getText().trim());

        CrawlConfig config = new CrawlConfig(url, topic, depth, maxPages);

        clearResults();
        startButton.setEnabled(false);
        resultCount.set(0);
        searchPanel.setVisible(false);
        crawlStartTime = System.currentTimeMillis();

        showLoadingInResults();

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                CrawlManager crawlManager = new CrawlManager(
                        config,
                        match -> SwingUtilities.invokeLater(() -> {
                            appendWithHighlight(String.format("[%d] %s\nSource: %s\n\n",
                                    resultCount.incrementAndGet(),
                                    match.getSentence(),
                                    match.getSourceUrl()), config.getTopic());
                            statusLabel.setText(String.format("Found %d matches so far...", resultCount.get()));
                            showResultsOnce(); // switch back to the text pane on first result
                        }),
                        pagesProcessed -> { /* no-op */ }
                );
                crawlManager.startCrawl();
                return null;
            }

            @Override
            protected void done() {
                long elapsedMillis = System.currentTimeMillis() - crawlStartTime;
                double elapsedSeconds = elapsedMillis / 1000.0;

                SwingUtilities.invokeLater(() -> {
                    // Ensure we're showing the results view even if there were zero matches
                    showResultsOnFinish();
                    startButton.setEnabled(true);
                    statusLabel.setText(String.format("Found %d matches in %.2f seconds", resultCount.get(), elapsedSeconds));

                    if (searchPanel.getParent() == null) {
                        centerPanel.add(searchPanel, BorderLayout.NORTH);
                    }
                    searchPanel.setVisible(true);

                    try {
                        documentReference.insertString(documentReference.getLength(), "\nCrawl complete!\n", defaultStyle);
                    } catch (BadLocationException ex) {
                        ex.printStackTrace();
                    }

                    revalidate();
                    repaint();
                });
            }
        };

        worker.execute();
    }

    private void showLoadingInResults() {
        resultsShownOnce = false;
        resultsScroll.setViewportView(loadingPanel);
        resultsScroll.getViewport().revalidate();
        resultsScroll.getViewport().repaint();
    }

    private void showResultsOnce() {
        if (resultsShownOnce) return;
        resultsShownOnce = true;
        resultsScroll.setViewportView(resultsArea);
        resultsScroll.getViewport().revalidate();
        resultsScroll.getViewport().repaint();
    }

    private void showResultsOnFinish() {
        resultsScroll.setViewportView(resultsArea);
        resultsScroll.getViewport().revalidate();
        resultsScroll.getViewport().repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CrawlerApp app = new CrawlerApp();
            app.setVisible(true);
        });
    }
}
