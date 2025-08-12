# Topic-Focused Summarizing Web Crawler

## Overview
This Java desktop application crawls a website starting from a user-provided seed URL, follows internal links up to a specified depth, and extracts sentences containing a keyword.

The application runs concurrently for faster page retrieval, highlights matching sentences in the GUI, and provides a browsable index of all visited pages.

The intent is to demonstrate multithreaded crawling, HTML parsing, keyword filtering, and data presentation using a Swing GUI.

## How It Works

### 1. Crawl Initialization
- The user provides:
  - **Seed URL** – starting point for the crawl
  - **Keyword** – the search term to find in sentences
  - **Depth limit** – how many link levels deep to follow
  - **Max pages** – total number of pages to visit
- Crawler starts at the seed URL and follows **internal links only**.
- Links are normalized to avoid duplicates:
  - Remove default ports (`:80` for HTTP, `:443` for HTTPS)
  - Resolve relative paths to absolute URLs

### 2. Concurrent Crawling
- Each discovered link is processed in its own thread via `ExecutorService`.
- `ConcurrentHashMap` and synchronized lists prevent revisiting the same URL.
- For each page:
  1. A `HEAD` request logs HTTP status, content length, and content type.
  2. The page is processed with Jsoup.
  3. Sentences containing the keyword are extracted and sent to the GUI in real time.

### 3. Data Logging & Browsable Index
- Every visited page is recorded in the crawl log:
  - URL
  - Parent URL
  - Depth
  - HTTP status
  - Size in bytes
  - Content type
- A **Crawl Index** dialog shows all records in a sortable table.

### 4. GUI
The Swing interface includes:
- Input fields for crawl parameters
- Start button
- Scrollable results area with highlighted keywords
- Button to open the crawl index table

## Code Overview
```
├── pom.xml
├── README.md
├── src
│   ├── main
│   │   └── java
│   │       ├── crawler
│   │       │   ├── CrawlManager.java # Manages tasks, deduplication, logging
│   │       │   └── CrawlTask.java # Processes a single page, finds matches, extracts links
│   │       ├── gui
│   │       │   └── CrawlerApp.java  # Main GUI and entry point
│   │       ├── model
│   │       │   ├── CrawlConfig.java # Stores crawl parameters
│   │       │   ├── LinkRecord.java # Metadata for each visited page
│   │       │   └── SentenceMatch.java # Matched sentence + source URL
│   │       └── parser
│   │           └── HTMLParser.java # Extracts keyword-containing sentences from the page HTML

```
(Layout tree generated using the linux package https://www.linuxfromscratch.org/blfs/view/svn/general/tree.html)


## Installation

### Requirements
- Java 8
- Maven

### Build
Pre-requisite: you must have Maven installed. If you don't, please refer to https://maven.apache.org/install.html.

```
mvn clean install
```

### Import into Eclipse
Go to `File -> Import -> Existing Maven Projects`, select the project directory, and click 'Finish'

### Running the Application
From Eclipse, open `src/main/java/gui/CrawlerApp.java`, right-click the file in the projecr explorer and click `Run As -> Java Application`

### Usage
Enter:

Seed URL - starting crawl point

Keyword - term to search for in sentences

Depth limit - link levels to follow

Max pages - maximum pages to visit

Click 'Start', watch the results appear with the keyword highlighted, and browse the index to see all visited pages with metadata.

### Demo
![Demo](Demo.gif)

### Limitations
* While the crawler avoids revisiting exact duplicate URLs, it does not detect near-duplicate content served from different URLs.
* Pages that require authentication, block crawlers, or implement rate-limiting (e.g., HTTP 429) may not be fully processed. For example, some Wikipedia special pages returned 429 Too Many Requests during testing.
* The crawler uses Jsoup for HTML parsing, which only processes static HTML. Content loaded dynamically via JavaScript will not be captured.
* Matching is case-insensitive but literal. Variations of a keyword (e.g., plural forms) may be missed.

### References
This is just a list of documentation of classes I ended up looking into and using for various purposes in the project:
* java.util.concurrent.ExecutorService - https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ExecutorService.html
* java.util.concurrent.Executors - https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/Executors.html
* java.util.concurrent.ConcurrentHashMap - https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/ConcurrentHashMap.html
* java.util.concurrent.atomic.AtomicInteger - https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicInteger.html
* java.util.function.Consumer - https://docs.oracle.com/javase/8/docs/api/java/util/function/Consumer.html
* java.util.regex.Pattern - https://docs.oracle.com/javase/8/docs/api/java/util/regex/Pattern.html
* java.util.regex.Matcher - https://docs.oracle.com/javase/8/docs/api/java/util/regex/Matcher.html
* java.net.URL - https://docs.oracle.com/javase/8/docs/api/java/net/URL.html
* java.net.URI - https://docs.oracle.com/javase/8/docs/api/java/net/URI.html
* java.net.HttpURLConnection - https://docs.oracle.com/javase/8/docs/api/java/net/HttpURLConnection.html
* javax.swing.JFrame - https://docs.oracle.com/javase/8/docs/api/javax/swing/JFrame.html
* javax.swing.JPanel - https://docs.oracle.com/javase/8/docs/api/javax/swing/JPanel.html
* javax.swing.JLabel - https://docs.oracle.com/javase/8/docs/api/javax/swing/JLabel.html
* javax.swing.JButton - https://docs.oracle.com/javase/8/docs/api/javax/swing/JButton.html
* javax.swing.JTextField - https://docs.oracle.com/javase/8/docs/api/javax/swing/JTextField.html
* javax.swing.JTextPane - https://docs.oracle.com/javase/8/docs/api/javax/swing/JTextPane.html
* javax.swing.JScrollPane - https://docs.oracle.com/javase/8/docs/api/javax/swing/JScrollPane.html
* javax.swing.JTable - https://docs.oracle.com/javase/8/docs/api/javax/swing/JTable.html
* javax.swing.JOptionPane - https://docs.oracle.com/javase/8/docs/api/javax/swing/JOptionPane.html
* javax.swing.SwingWorker - https://docs.oracle.com/javase/8/docs/api/javax/swing/SwingWorker.html
* javax.swing.SwingUtilities - https://docs.oracle.com/javase/8/docs/api/javax/swing/SwingUtilities.html
* javax.swing.BorderFactory - https://docs.oracle.com/javase/8/docs/api/javax/swing/BorderFactory.html
* javax.swing.Box - https://docs.oracle.com/javase/8/docs/api/javax/swing/Box.html
* javax.swing.table.DefaultTableCellRenderer - https://docs.oracle.com/javase/8/docs/api/javax/swing/table/DefaultTableCellRenderer.html
* javax.swing.text.StyledDocument - https://docs.oracle.com/javase/8/docs/api/javax/swing/text/StyledDocument.html
* javax.swing.text.Style - https://docs.oracle.com/javase/8/docs/api/javax/swing/text/Style.html
* javax.swing.text.Highlighter - https://docs.oracle.com/javase/8/docs/api/javax/swing/text/Highlighter.html
* javax.swing.text.DefaultHighlighter - https://docs.oracle.com/javase/8/docs/api/javax/swing/text/DefaultHighlighter.html
* java.awt.BorderLayout - https://docs.oracle.com/javase/8/docs/api/java/awt/BorderLayout.html
* java.awt.FlowLayout - https://docs.oracle.com/javase/8/docs/api/java/awt/FlowLayout.html
* javax.swing.BoxLayout - https://docs.oracle.com/javase/8/docs/api/javax/swing/BoxLayout.html
* org.jsoup.Jsoup - https://jsoup.org/apidocs/org/jsoup/Jsoup.html
* org.jsoup.nodes.Document - https://jsoup.org/apidocs/org/jsoup/nodes/Document.html
* org.jsoup.nodes.Element - https://jsoup.org/apidocs/org/jsoup/nodes/Element.html
* org.jsoup.select.Elements - https://jsoup.org/apidocs/org/jsoup/select/Elements.html
