package com.resumeanalyzer;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class KeywordMatcher {

    public static class MatchResult {
        public final List<String> foundKeywords;
        public final List<String> missingKeywords;
        public final double scorePercentage;

        public MatchResult(List<String> foundKeywords, List<String> missingKeywords, double scorePercentage) {
            this.foundKeywords = foundKeywords;
            this.missingKeywords = missingKeywords;
            this.scorePercentage = scorePercentage;
        }
    }

    public static List<String> loadKeywordsFromFile(String path) throws IOException {
        if (path == null || path.isEmpty()) return loadDefaultKeywords();
        return Files.readAllLines(Paths.get(path)).stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    public static List<String> loadDefaultKeywords() throws IOException {
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("keywords.txt");
        if (is == null) return new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            return br.lines().map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        }
    }

    public static MatchResult match(String resumeText, List<String> keywords) {
        String lower = resumeText == null ? "" : resumeText.toLowerCase();
        Set<String> found = new HashSet<>();
        for (String kw : keywords) {
            String k = kw.toLowerCase();
            if (lower.contains(k)) found.add(kw);
        }
        List<String> foundList = new ArrayList<>(found);
        List<String> missing = new ArrayList<>();
        for (String kw : keywords) if (!found.contains(kw)) missing.add(kw);
        double score = keywords.isEmpty() ? 0 : (foundList.size() * 100.0 / keywords.size());
        return new MatchResult(foundList, missing, score);
    }
}
