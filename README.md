# Topic-Focused Summarizing Web Crawler
## Overview
This Java desktop application crawls a website starting from a user-provided seed URL, follows internal links up to a specified depth, and extracts sentences containing a keyword. Results are displayed in a Swing-based GUI with the keyword highlighted.

The crawler runs concurrently using multiple threads for faster page retrieval and provides real-time progress updates.

## Features
* Customizable crawl settings:
  * Seed URL
  * Keyword
  * Crawl depth limit
  * Maximum number of pages

* Parallelized crawling using ExecutorService
* Robust HTML parsing with Jsoup for:
  * Extracting visible text
  * Splitting into sentences
  * Filtering sentences containing the keyword

* Interactive GUI:

  * Input fields for all crawl parameters
  * Start button and progress bar
  * Scrollable results with keyword highlighting
 * Status updates (e.g., number of matches found)

* Error handling for HTTP failures and timeouts

## Technologies Used
* Java 8
* Swing (GUI)
* Jsoup (HTML parsing)
* Java Concurrency API (ExecutorService, ConcurrentHashMap)

## Installation

Make sure you have Maven installed, then run:
`mvn clean install`

1. mport the project into Eclipse (File -> Import -> Existing Maven Projects).
2. `cd topic-focused-web-crawler`
3. Build with Maven:
`mvn package`

# Running the Application


From inside Eclipse, look at `/src/main/java/gui/CrawlerApp.java`. Right-click, and then select `Run As -> Java Application`.

Enter:

Seed URL – Starting point for the crawl

Keyword – Text to search for in sentences

Depth limit – How many link levels deep to crawl

Max pages – Limit total pages visited

Click Start.

Watch progress in the bar and see results appear in real-time with highlighted keywords.

Example
Seed URL: https://example.com
Keyword: data
Depth limit: 2
Max pages: 50

Output:


"Found <number-of-matches> matches
Crawl complete!"  with the results pane showing each matched sentence and its source URL.