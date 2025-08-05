package gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.List;

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
    private final JProgressBar progressBar;
    private final JLabel progressLabel;
    private final StyledDocument documentReference;
    private Style defaultStyle;
    private long crawlStartTime;


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

        progressBar = new JProgressBar();
        progressBar.setMinimum(0);
        progressBar.setMaximum(100); // Fixed: Always use 0-100 for percentage
        progressBar.setStringPainted(false);

        progressLabel = new JLabel("0%");

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

        resultsArea = new JTextPane();

        this.documentReference = this.resultsArea.getStyledDocument();
        this.defaultStyle = this.resultsArea.getStyle("DefaultStyle");

        resultsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Crawl Results"));

        JPanel top = new JPanel(new BorderLayout());
        top.add(inputPanel, BorderLayout.CENTER);

        JPanel bottom = new JPanel(new BorderLayout());
        top.add(bottom, BorderLayout.SOUTH);

        bottom.add(startButton, BorderLayout.NORTH);

        JPanel progressPanel = new JPanel(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.add(progressLabel, BorderLayout.EAST);
        bottom.add(progressPanel, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void appendWithHighlight(String sentence, String keyword) {
        // Ensure keyword style is created and properly configured
        if (defaultStyle == null) {
            defaultStyle = this.resultsArea.addStyle("DefaultStyle", null);
            StyleConstants.setForeground(defaultStyle, Color.BLACK);
            StyleConstants.setBold(defaultStyle, false);
        }

        Style keywordStyle = this.resultsArea.getStyle("KeywordStyle");
        if (keywordStyle == null) {
            keywordStyle = this.resultsArea.addStyle("KeywordStyle", null);
            StyleConstants.setForeground(keywordStyle, Color.GREEN);
            StyleConstants.setBold(keywordStyle, true);
        }

        int lastIndex = 0;
        String lowerSentence = sentence.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        while (lastIndex < sentence.length()) {
            int index = lowerSentence.indexOf(lowerKeyword, lastIndex);
            if (index == -1) {
                // Append remaining text using default style
                try {
                    this.documentReference.insertString(this.documentReference.getLength(), sentence.substring(lastIndex), defaultStyle);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                break;
            }

            // Append text before the keyword
            try {
                documentReference.insertString(this.documentReference.getLength(), sentence.substring(lastIndex, index), defaultStyle);
                this.documentReference.insertString(this.documentReference.getLength(), sentence.substring(index, index + keyword.length()), keywordStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }

            lastIndex = index + keyword.length();
        }

        try {
            this.documentReference.insertString(this.documentReference.getLength(), "\n\n", defaultStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    // Fixed: Add method to clear results without destroying formatting
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

        progressBar.setValue(0);
        progressLabel.setText("0%");

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() {
                CrawlManager crawlManager = new CrawlManager(
                        config,
                        match -> SwingUtilities.invokeLater(() -> {
                            appendWithHighlight(String.format("[%d] %s\n    Source: %s\n\n",
                                    resultCount.incrementAndGet(),
                                    match.getSentence(),
                                    match.getSourceUrl()), config.getTopic());

                            statusLabel.setText(String.format("Found %d matches so far...", resultCount.get()));
                        }),
                        progress -> {
                            int percentage = Math.min(100, (int) ((progress / (double) maxPages) * 100));
                            publish(percentage);
                        }
                );
                crawlManager.startCrawl();
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {
                int latestProgress = chunks.get(chunks.size() - 1);
                progressBar.setValue(latestProgress);
                progressLabel.setText(latestProgress + "%");
            }

            @Override
            protected void done() {
                final long elapsedMillis = System.currentTimeMillis() - crawlStartTime;
                final double elapsedSeconds = elapsedMillis / 1000.0;

                SwingUtilities.invokeLater(() -> {
                    progressBar.setValue(100);
                    progressLabel.setText("100%");
                    startButton.setEnabled(true);
                    statusLabel.setText(String.format("Found %d matches in %.2f seconds", resultCount.get(), elapsedSeconds));

                    try {
                        documentReference.insertString(documentReference.getLength(), "\nCrawl complete!\n", defaultStyle);
                    } catch (BadLocationException e) {
                        e.printStackTrace();
                    }
                });
            }
        };
        this.crawlStartTime = System.currentTimeMillis();

        worker.execute();
    }



    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CrawlerApp app = new CrawlerApp();
            app.setVisible(true);
        });
    }
}