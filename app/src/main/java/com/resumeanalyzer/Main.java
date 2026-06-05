package com.resumeanalyzer;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in)) {
            while (true) {
                printMenu();
                String choice = sc.nextLine().trim();
                switch (choice) {
                    case "1":
                        analyzeResume(sc);
                        break;
                    case "2":
                        viewPastResults();
                        break;
                    case "3":
                    case "":
                        System.out.println("Goodbye.");
                        return;
                    default:
                        System.out.println("Invalid option. Enter 1, 2, or 3.");
                }
            }
        }
    }

    private static void printMenu() {
        System.out.println("\n=== ResumeAnalyzer Menu ===");
        System.out.println("1) Analyze a resume");
        System.out.println("2) View past analysis results");
        System.out.println("3) Exit");
        System.out.print("Select an option: ");
    }

    private static void analyzeResume(Scanner sc) {
        System.out.println("Enter path to resume file (.pdf or .txt):");
        String path = sc.nextLine().trim();
        File resumeFile = new File(path);

        if (!resumeFile.exists()) {
            System.err.println("File not found: " + path);
            return;
        }

        LOGGER.info("Parsing resume file: " + path);
        String resumeText;
        try {
            resumeText = ResumeParser.extractText(path);
        } catch (IOException e) {
            System.err.println("Failed to read resume: " + e.getMessage());
            return;
        }

        List<String> keywords;
        try {
            keywords = KeywordMatcher.loadDefaultKeywords();
            if (keywords.isEmpty()) {
                System.err.println("No keywords found in resources/keywords.txt");
                return;
            }
        } catch (IOException e) {
            System.err.println("Failed to load keywords: " + e.getMessage());
            return;
        }

        KeywordMatcher.MatchResult result = KeywordMatcher.match(resumeText, keywords);
        LOGGER.info(String.format("Analysis complete for %s: %.2f%% match", resumeFile.getName(), result.scorePercentage));

        System.out.println("\n--- Resume Analysis Report ---");
        System.out.printf("Resume name: %s%n", resumeFile.getName());
        System.out.printf("Match percentage: %.2f%%%n", result.scorePercentage);
        System.out.printf("Matched keywords: %d/%d%n", result.foundKeywords.size(), keywords.size());

        if (result.missingKeywords.isEmpty()) {
            System.out.println("Missing keywords: None");
        } else {
            System.out.println("Missing keywords:");
            result.missingKeywords.forEach(keyword -> System.out.println(" - " + keyword));
        }

        List<String> suggestions = FeedbackGenerator.generateSuggestions(resumeText, result.missingKeywords);
        System.out.println("\nSuggestions:");
        suggestions.forEach(suggestion -> System.out.println(" - " + suggestion));

        try {
            DatabaseHandler db = new DatabaseHandler();
            db.saveResult(resumeFile.getName(), result.scorePercentage);
            System.out.println("\nConfirmation: analysis results saved to the database table 'analysis_results'.");
            LOGGER.info("Results saved to database for resume: " + resumeFile.getName());
        } catch (IOException | SQLException e) {
            System.err.println("Failed to save to DB: " + e.getMessage());
        }
    }

    private static void viewPastResults() {
        try {
            DatabaseHandler db = new DatabaseHandler();
            List<DatabaseHandler.AnalysisResult> results = db.fetchAnalysisResults();
            if (results.isEmpty()) {
                System.out.println("No past analysis results found.");
            } else {
                System.out.println("\n--- Past Analysis Results ---");
                results.forEach(result -> System.out.printf("[%d] %s | %.2f%% | %s%n",
                        result.id,
                        result.resumeName,
                        result.matchScore,
                        result.analysisDate));
            }
            LOGGER.info("Viewed past analysis results.");
        } catch (IOException | SQLException e) {
            System.err.println("Unable to view past results: " + e.getMessage());
        }
    }
}
