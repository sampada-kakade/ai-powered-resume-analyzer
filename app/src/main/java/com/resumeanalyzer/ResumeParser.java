package com.resumeanalyzer;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class ResumeParser {

    public static String extractText(String filePath) throws IOException {
        if (filePath == null) return "";
        String lower = filePath.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return extractFromPdf(filePath);
        } else if (lower.endsWith(".txt")) {
            return new String(Files.readAllBytes(Paths.get(filePath)));
        } else {
            throw new IOException("Unsupported file type. Use .pdf or .txt");
        }
    }

    private static String extractFromPdf(String filePath) throws IOException {
        try (PDDocument document = PDDocument.load(new File(filePath))) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }
}
