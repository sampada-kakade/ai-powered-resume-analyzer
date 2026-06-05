package com.resumeanalyzer;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.tokenize.SimpleTokenizer;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NLPAnalyzer {
    private static final Logger LOGGER = Logger.getLogger(NLPAnalyzer.class.getName());
    private static final List<String> WEAK_PHRASES = List.of(
            "responsible for", "assisted", "helped", "participated in", "worked on", "tasked with"
    );

    public static class Result {
        public final int actionVerbCount;
        public final int quantifiedAchievements;
        public final List<String> weakPhrasesFound;

        public Result(int actionVerbCount, int quantifiedAchievements, List<String> weakPhrasesFound) {
            this.actionVerbCount = actionVerbCount;
            this.quantifiedAchievements = quantifiedAchievements;
            this.weakPhrasesFound = weakPhrasesFound;
        }
    }

    public static Result analyze(String text) {
        if (text == null) text = "";
        int verbCount = 0;
        int quantified = 0;
        List<String> weak = new ArrayList<>();

        // count numbers as quantified achievements
        Pattern numberPattern = Pattern.compile("\\b\\d{1,3}(?:,\\d{3})*(?:\\.\\d+)?\\b");
        Matcher m = numberPattern.matcher(text);
        while (m.find()) quantified++;

        // detect weak phrases
        String lower = text.toLowerCase(Locale.ROOT);
        for (String p : WEAK_PHRASES) if (lower.contains(p)) weak.add(p);

        // try OpenNLP POS tagging to count verbs (falls back to heuristic)
        try (InputStream modelIn = Thread.currentThread().getContextClassLoader().getResourceAsStream("en-pos-maxent.bin")) {
            if (modelIn != null) {
                POSModel model = new POSModel(modelIn);
                POSTaggerME tagger = new POSTaggerME(model);
                SimpleTokenizer tokenizer = SimpleTokenizer.INSTANCE;
                String[] tokens = tokenizer.tokenize(text);
                String[] tags = tagger.tag(tokens);
                for (String tag : tags) {
                    if (tag.startsWith("VB")) verbCount++;
                }
                return new Result(verbCount, quantified, weak);
            } else {
                LOGGER.info("POS model not found; using heuristic verb count");
            }
        } catch (IOException e) {
            LOGGER.warning("OpenNLP POS tagging failed: " + e.getMessage());
        }

        // heuristic: count common action verbs
        String[] commonVerbs = new String[]{"achieved","improved","developed","led","implemented","designed","launched","created","built","optimized","managed","reduced","increased","delivered"};
        for (String v : commonVerbs) {
            Pattern p = Pattern.compile("\\b" + Pattern.quote(v) + "\\b", Pattern.CASE_INSENSITIVE);
            Matcher mm = p.matcher(text);
            while (mm.find()) verbCount++;
        }

        return new Result(verbCount, quantified, weak);
    }
}
