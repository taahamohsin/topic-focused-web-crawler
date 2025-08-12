package gui;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import model.CrawlConfig;
import model.LinkRecord;
import crawler.CrawlManager;

public class CrawlerApp extends JFrame {
    private final JTextField urlField;
    private final JTextField topicField;
    private final JTextField depthField;
    private final JTextField maxPagesField;
    private final JButton startButton;
    private final JButton showIndexButton;
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
    private final JScrollPane resultsScroll;
    private final JPanel loadingPanel;
    private boolean resultsShownOnce = false;
    private CrawlManager lastCrawlManager;

    public CrawlerApp() {
        super("Topic-Focused Web Crawler");

        setLayout(new BorderLayout(10, 10));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(800, 500);

        JPanel inputPanel = new JPanel();
        inputPanel.setLayout(new BoxLayout(inputPanel, BoxLayout.Y_AXIS));
        inputPanel.setBorder(BorderFactory.createTitledBorder("Crawl Settings"));

        JPanel urlRow = new JPanel(new BorderLayout(8, 0));
        JLabel urlLabel = new JLabel("Seed URL:");
        urlField = new JTextField("https://example.com");
        urlRow.add(urlLabel, BorderLayout.WEST);
        urlRow.add(urlField, BorderLayout.CENTER);

        JPanel topicRow = new JPanel(new BorderLayout(8, 0));
        JLabel topicLabel = new JLabel("Topic:");
        topicField = new JTextField("example", 28);
        topicRow.add(topicLabel, BorderLayout.WEST);
        topicRow.add(topicField, BorderLayout.CENTER);

        JPanel paramsRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        JLabel depthLabel = new JLabel("Depth Limit:");
        depthField = new JTextField("1", 4);
        JLabel maxPagesLabel = new JLabel("Max Pages:");
        maxPagesField = new JTextField("10", 4);

        startButton = new JButton("Start");
        startButton.setMargin(new Insets(2, 8, 2, 8));
        startButton.setPreferredSize(new Dimension(90, 26));
        startButton.addActionListener(this::handleStart);

        showIndexButton = new JButton("Show Index");
        showIndexButton.setEnabled(false);
        showIndexButton.addActionListener(e -> showIndexDialog());

        paramsRow.add(depthLabel);
        paramsRow.add(depthField);
        paramsRow.add(maxPagesLabel);
        paramsRow.add(maxPagesField);
        paramsRow.add(Box.createHorizontalStrut(12));
        paramsRow.add(startButton);
        paramsRow.add(showIndexButton);

        inputPanel.add(urlRow);
        inputPanel.add(Box.createVerticalStrut(6));
        inputPanel.add(topicRow);
        inputPanel.add(Box.createVerticalStrut(6));
        inputPanel.add(paramsRow);

        resultsArea = new JTextPane();
        resultsArea.setEditable(false);
        documentReference = resultsArea.getStyledDocument();

        defaultStyle = resultsArea.addStyle("DefaultStyle", null);
        StyleConstants.setForeground(defaultStyle, Color.BLACK);

        resultsScroll = new JScrollPane(resultsArea);
        resultsScroll.setBorder(BorderFactory.createTitledBorder("Crawl Results"));

        loadingPanel = new JPanel(new BorderLayout());
        JLabel loadingLabel = new JLabel("Currently crawling…", SwingConstants.CENTER);
        loadingPanel.add(loadingLabel, BorderLayout.CENTER);

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

        centerPanel = new JPanel(new BorderLayout());
        centerPanel.add(resultsScroll, BorderLayout.CENTER);

        JPanel top = new JPanel(new BorderLayout());
        top.add(inputPanel, BorderLayout.CENTER);

        statusLabel = new JLabel();

        add(top, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(statusLabel, BorderLayout.SOUTH);
    }

    private void findNextMatch() {
        String content;
        try {
            content = documentReference.getText(0, documentReference.getLength()).toLowerCase();
        } catch (BadLocationException e) {
            return;
        }
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

    private void appendWithHighlight(String sentence, String keyword, String sourceUrl) {
        Style keywordStyle = resultsArea.getStyle("KeywordStyle");
        if (keywordStyle == null) {
            keywordStyle = resultsArea.addStyle("KeywordStyle", null);
            StyleConstants.setForeground(keywordStyle, new Color(0x1a7f37));
            StyleConstants.setBold(keywordStyle, true);
        }

        int lastIndex = 0;
        String lowerSentence = sentence.toLowerCase();
        String lowerKeyword = keyword.toLowerCase();

        while (lastIndex < sentence.length()) {
            int index = lowerSentence.indexOf(lowerKeyword, lastIndex);
            if (index == -1) {
                try {
                    documentReference.insertString(documentReference.getLength(),
                            sentence.substring(lastIndex), defaultStyle);
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                break;
            }
            try {
                documentReference.insertString(documentReference.getLength(),
                        sentence.substring(lastIndex, index), defaultStyle);
                documentReference.insertString(documentReference.getLength(),
                        sentence.substring(index, index + keyword.length()), keywordStyle);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
            lastIndex = index + keyword.length();
        }

        try {
            documentReference.insertString(documentReference.getLength(),
                    "\nSource: " + sourceUrl + "\n\n", defaultStyle);
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
        showIndexButton.setEnabled(false);
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
                            appendWithHighlight(
                                    String.format("[%d] %s",
                                            resultCount.incrementAndGet(),
                                            match.getSentence()),
                                    config.getTopic(),
                                    match.getSourceUrl()
                            );
                            statusLabel.setText(String.format("Found %d matches so far...", resultCount.get()));
                            showResultsOnce();
                        }),
                        pagesprocessed -> { /* do nothing */ }
                );
                lastCrawlManager = crawlManager;
                crawlManager.startCrawl();
                return null;
            }

            @Override
            protected void done() {
                long elapsedMillis = System.currentTimeMillis() - crawlStartTime;
                double elapsedSeconds = elapsedMillis / 1000.0;

                SwingUtilities.invokeLater(() -> {
                    showResultsOnFinish();
                    startButton.setEnabled(true);
                    showIndexButton.setEnabled(true);
                    statusLabel.setText(String.format("Found %d matches in %.2f seconds",
                            resultCount.get(), elapsedSeconds));

                    if (searchPanel.getParent() == null) {
                        centerPanel.add(searchPanel, BorderLayout.NORTH);
                    }
                    searchPanel.setVisible(true);

                    try {
                        documentReference.insertString(documentReference.getLength(),
                                "Crawl complete!\n", defaultStyle);
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

    private void showIndexDialog() {
        if (lastCrawlManager == null) return;
        List<LinkRecord> log = lastCrawlManager.getCrawlLog();

        String[] cols = {"URL", "Depth", "Parent", "Status", "Size", "Type"};
        Object[][] data;
        synchronized (log) {
            data = new Object[log.size()][cols.length];
            for (int i = 0; i < log.size(); i++) {
                LinkRecord r = log.get(i);
                data[i][0] = r.url;
                data[i][1] = r.depth;
                data[i][2] = r.parentUrl == null ? "" : r.parentUrl;
                data[i][3] = r.status;
                data[i][4] = r.sizeBytes >= 0 ? r.sizeBytes : "";
                data[i][5] = r.contentType == null ? "" : r.contentType;
            }
        }
        JTable table = new JTable(data, cols);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);

        int[] widths = { 430, 60, 430, 60, 100, 150 }; // URL, Depth, Parent, Status, Size, Type respectively
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setPreferredSize(new Dimension(1000, 560));
        JOptionPane.showMessageDialog(this, sp, "Crawl Index", JOptionPane.PLAIN_MESSAGE);

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
