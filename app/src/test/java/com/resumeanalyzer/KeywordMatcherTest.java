package com.resumeanalyzer;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class KeywordMatcherTest {

    @Test
    void shouldMatchKeywordsFromSampleResume() throws IOException {
        String resumeText;
        List<String> keywords;
        try (InputStream input = getClass().getResourceAsStream("/sample_resume.txt")) {
            assertNotNull(input, "Test resource sample_resume.txt must be available on the classpath");
            resumeText = new String(input.readAllBytes(), StandardCharsets.UTF_8);
        }

        keywords = KeywordMatcher.loadDefaultKeywords();
        assertFalse(keywords.isEmpty(), "keywords.txt must not be empty");

        KeywordMatcher.MatchResult result = KeywordMatcher.match(resumeText, keywords);

        assertTrue(result.scorePercentage > 0, "Expected positive match score for sample resume");
        assertTrue(result.foundKeywords.contains("Java"), "Expected 'Java' to be matched");
        assertTrue(result.foundKeywords.contains("AWS"), "Expected 'AWS' to be matched");
        assertTrue(result.foundKeywords.contains("Docker"), "Expected 'Docker' to be matched");
        assertFalse(result.missingKeywords.contains("Java"), "Java should not be missing");
        assertTrue(result.missingKeywords.size() < keywords.size(), "Some keywords should be found");
    }
}
