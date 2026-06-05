package com.resumeanalyzer.api;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
public class AnalyzeController {

    @PostMapping(value = "/analyze", consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String,Object> analyzeText(@RequestBody String text) throws IOException {
        Map<String,Object> resp = new HashMap<>();
        List<String> keywords = com.resumeanalyzer.KeywordMatcher.loadDefaultKeywords();
        com.resumeanalyzer.KeywordMatcher.MatchResult result = com.resumeanalyzer.KeywordMatcher.match(text, keywords);
        List<String> suggestions = com.resumeanalyzer.FeedbackGenerator.generateSuggestions(text, result.missingKeywords);
        resp.put("score", result.scorePercentage);
        resp.put("found", result.foundKeywords);
        resp.put("missing", result.missingKeywords);
        resp.put("suggestions", suggestions);
        return resp;
    }

    @GetMapping(value = "/results", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String,Object>> results() {
        try {
            com.resumeanalyzer.DatabaseHandler dh = new com.resumeanalyzer.DatabaseHandler();
            List<com.resumeanalyzer.DatabaseHandler.AnalysisResult> rows = dh.fetchAnalysisResults();
            return rows.stream().map(r -> {
                Map<String,Object> m = new HashMap<>();
                m.put("id", r.id);
                m.put("resumeName", r.resumeName);
                m.put("score", r.matchScore);
                m.put("date", r.analysisDate);
                return m;
            }).toList();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
