package gui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import model.CrawlConfig;
import crawler.CrawlManager;

public class CrawlerApp extends JFrame {
    private JTextField urlField;
    private JTextField topicField;
    private JTextField depthField;
    private JTextField maxPagesField;
    private JButton startButton;
    private JTextArea resultsArea;

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

        resultsArea = new JTextArea();
        resultsArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(resultsArea);
        scrollPane.setBorder(BorderFactory.createTitledBorder("Crawl Results"));

        JPanel top = new JPanel(new BorderLayout());
        top.add(inputPanel, BorderLayout.CENTER);
        top.add(startButton, BorderLayout.SOUTH);

        add(top, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);
    }


    private void handleStart(ActionEvent e) {
        String url = urlField.getText().trim();
        String topic = topicField.getText().trim();
        int depth = Integer.parseInt(depthField.getText().trim());
        int maxPages = Integer.parseInt(maxPagesField.getText().trim());

        CrawlConfig config = new CrawlConfig(url, topic, depth, maxPages);
        resultsArea.setText("Starting crawl...\n");


        // Run in background so GUI stays responsive
        new Thread(() -> {
            CrawlManager manager = new CrawlManager(config, match -> {
                SwingUtilities.invokeLater(() -> {
                    resultsArea.append("- " + match.getSentence() + "\n  [Source: " + match.getSourceUrl() + "]\n\n");
                });
            });

            manager.startCrawl();

            SwingUtilities.invokeLater(() -> resultsArea.append("\nCrawl complete!\n"));
        }).start();

    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            CrawlerApp app = new CrawlerApp();
            app.setVisible(true);
        });
    }
}