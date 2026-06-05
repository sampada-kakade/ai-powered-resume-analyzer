package com.resumeanalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import java.util.logging.Logger;

import static com.resumeanalyzer.NLPAnalyzer.Result;

public class FeedbackGenerator {

    private static final List<String> ACTION_VERBS = Arrays.asList(
            "achieved", "improved", "developed", "led", "implemented", "designed", "launched", "created", "built", "optimized"
    );

    private static final Logger LOGGER = Logger.getLogger(FeedbackGenerator.class.getName());

    public static List<String> generateSuggestions(String resumeText, List<String> missingKeywords) {
        List<String> suggestions = new ArrayList<>();

        // NLP analysis
        Result nlp = NLPAnalyzer.analyze(resumeText);
        LOGGER.info("NLP analysis: verbs=" + nlp.actionVerbCount + ", quantified=" + nlp.quantifiedAchievements + ", weakPhrases=" + nlp.weakPhrasesFound.size());

        // Suggest adding technical skills if many keywords missing
        if (missingKeywords.size() > Math.max(3, missingKeywords.size() / 3)) {
            suggestions.add("Add more technical skills relevant to the job description");
        }

        // Action verbs
        if (nlp.actionVerbCount < 2) {
            suggestions.add("Use more action verbs (e.g., achieved, developed, led)");
        }

        // Measurable achievements
        if (nlp.quantifiedAchievements == 0) {
            suggestions.add("Include measurable achievements (numbers, percentages, metrics)");
        }

        // Weak phrases
        if (!nlp.weakPhrasesFound.isEmpty()) {
            suggestions.add("Avoid weak phrases: " + String.join(", ", nlp.weakPhrasesFound));
        }

        // General soft suggestion
        if (suggestions.isEmpty()) suggestions.add("Resume looks good; tailor it to the job description where possible");

        return suggestions;
    }
}
