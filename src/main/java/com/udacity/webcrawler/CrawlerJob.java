package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.locks.Lock;
import java.util.regex.Pattern;

public class CrawlerJob extends RecursiveTask<Boolean> {
    private String url;
    private Instant deadTime;
    private int maxDepth;
    private Map<String, Integer> counts;
    private Set<String> visitedUrls;
    private Lock lock;
    private Clock clock;
    private List<Pattern> ignoreUrlPatterns;
    private PageParserFactory parserFactory;

    public CrawlerJob(String url, Instant deadTime, int maxDepth, Map<String, Integer> counts, Set<String> visitedUrls, Lock lock, Clock clock, List<Pattern> ignoreUrlPatterns, PageParserFactory parserFactory) {
        this.url = url;
        this.deadTime = deadTime;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.visitedUrls = visitedUrls;
        this.lock = lock;
        this.clock = clock;
        this.ignoreUrlPatterns = ignoreUrlPatterns;
        this.parserFactory = parserFactory;
    }

    @Override
    protected Boolean compute() {
        boolean isIgnored = ignoreUrlPatterns.stream().anyMatch(pattern -> pattern.matcher(url).matches());

        if (isIgnored || maxDepth == 0 || clock.instant().isAfter(deadTime) || visitedUrls.contains(url)) {
            return false;
        }
        try {
            visitedUrls.add(url);
                PageParser.Result result = parserFactory.get(url).parse();
        for (Map.Entry<String, Integer> entry : result.getWordCounts().entrySet()) {
            String word = entry.getKey();
            Integer count = entry.getValue();
            Integer wordCount = counts.containsKey(word) ? count + counts.get(word) : count;
            counts.put(word, wordCount);
        }

        List<String> resultLinks = result.getLinks();
        List<CrawlerJob> jobs = new ArrayList<>();

        resultLinks.forEach(link -> jobs.add(new CrawlerJob(link, deadTime, maxDepth - 1, counts, visitedUrls, lock, clock, ignoreUrlPatterns, parserFactory)));
        invokeAll(jobs);
        } catch (Exception exception) {
            System.err.println("Crawler job failed to process url: " + url + ": " + exception);
        }
        return true;
    }
}
