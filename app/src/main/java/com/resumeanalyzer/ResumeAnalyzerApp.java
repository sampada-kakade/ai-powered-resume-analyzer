package com.resumeanalyzer;

import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Separator;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ResumeAnalyzerApp extends Application {
    private static final Logger LOGGER = Logger.getLogger(ResumeAnalyzerApp.class.getName());
    private static final String[] SUPPORTED_EXTENSIONS = {"*.txt", "*.pdf"};

    private TextArea resumeContentArea;
    private TextArea analysisOutputArea;
    private Label selectedFileLabel;
    private TableView<DatabaseHandler.AnalysisResult> pastResultsTable;
    private Button analyzeButton;
    private Button exportCsvButton;
    private Button exportPdfButton;

    private File selectedFile;
    private AnalysisSummary currentSummary;

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Resume Analyzer");
        BorderPane root = new BorderPane();
        root.setTop(createMenuBar(primaryStage));
        root.setCenter(createContentPane(primaryStage));
        root.setBottom(createExportToolbar(primaryStage));

        Scene scene = new Scene(root, 1000, 720);
        primaryStage.setScene(scene);
        primaryStage.show();

        refreshPastResults();
    }

    private MenuBar createMenuBar(Stage stage) {
        MenuBar menuBar = new MenuBar();
        Menu settingsMenu = new Menu("Settings");

        MenuItem dbSettings = new MenuItem("Configure DB...");
        dbSettings.setOnAction(event -> showSettingsDialog(stage));

        MenuItem refreshResults = new MenuItem("Refresh Past Results");
        refreshResults.setOnAction(event -> refreshPastResults());

        settingsMenu.getItems().addAll(dbSettings, refreshResults);
        menuBar.getMenus().add(settingsMenu);
        return menuBar;
    }

    private TabPane createContentPane(Stage stage) {
        TabPane tabPane = new TabPane();
        Tab analysisTab = new Tab("Analysis");
        analysisTab.setContent(createAnalysisPane(stage));
        analysisTab.setClosable(false);

        Tab pastResultsTab = new Tab("Past Results");
        pastResultsTab.setContent(createPastResultsPane());
        pastResultsTab.setClosable(false);

        tabPane.getTabs().addAll(analysisTab, pastResultsTab);
        return tabPane;
    }

    private VBox createAnalysisPane(Stage stage) {
        VBox analysisPane = new VBox(10);
        analysisPane.setPadding(new Insets(12));

        HBox topRow = new HBox(10);
        Button chooseFile = new Button("Choose Resume");
        chooseFile.setOnAction(event -> chooseResumeFile(stage));

        selectedFileLabel = new Label("No resume selected");
        HBox.setHgrow(selectedFileLabel, Priority.ALWAYS);
        selectedFileLabel.setMaxWidth(Double.MAX_VALUE);

        analyzeButton = new Button("Run Analysis");
        analyzeButton.setDisable(true);
        analyzeButton.setOnAction(event -> analyzeSelectedResume());

        topRow.getChildren().addAll(chooseFile, selectedFileLabel, analyzeButton);
        topRow.setSpacing(10);

        resumeContentArea = new TextArea();
        resumeContentArea.setPromptText("Parsed resume content will appear here...");
        resumeContentArea.setEditable(false);
        resumeContentArea.setWrapText(true);
        resumeContentArea.setPrefHeight(260);

        analysisOutputArea = new TextArea();
        analysisOutputArea.setPromptText("Analysis output will appear here...");
        analysisOutputArea.setEditable(false);
        analysisOutputArea.setWrapText(true);
        analysisOutputArea.setPrefHeight(260);

        analysisPane.getChildren().addAll(topRow, new Label("Resume Content"), resumeContentArea, new Separator(), new Label("Analysis Results"), analysisOutputArea);
        return analysisPane;
    }

    private VBox createPastResultsPane() {
        VBox pastPane = new VBox(10);
        pastPane.setPadding(new Insets(12));

        pastResultsTable = new TableView<>();
        TableColumn<DatabaseHandler.AnalysisResult, Integer> idColumn = new TableColumn<>("ID");
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        idColumn.setPrefWidth(50);

        TableColumn<DatabaseHandler.AnalysisResult, String> nameColumn = new TableColumn<>("Resume Name");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("resumeName"));
        nameColumn.setPrefWidth(220);

        TableColumn<DatabaseHandler.AnalysisResult, Double> scoreColumn = new TableColumn<>("Match %");
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("matchScore"));
        scoreColumn.setPrefWidth(100);

        TableColumn<DatabaseHandler.AnalysisResult, String> dateColumn = new TableColumn<>("Analysis Date");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("analysisDate"));
        dateColumn.setPrefWidth(220);

        pastResultsTable.getColumns().add(idColumn);
        pastResultsTable.getColumns().add(nameColumn);
        pastResultsTable.getColumns().add(scoreColumn);
        pastResultsTable.getColumns().add(dateColumn);
        pastResultsTable.setPrefHeight(520);

        Button exportAllCsvButton = new Button("Export Past Results CSV");
        exportAllCsvButton.setOnAction(event -> exportPastResultsToCsv());

        Button exportAllPdfButton = new Button("Export Past Results PDF");
        exportAllPdfButton.setOnAction(event -> exportPastResultsToPdf());

        HBox btnRow = new HBox(10);
        btnRow.getChildren().addAll(exportAllCsvButton, exportAllPdfButton);

        pastPane.getChildren().addAll(pastResultsTable, btnRow);
        return pastPane;
    }

    private HBox createExportToolbar(Stage stage) {
        HBox toolbar = new HBox(10);
        toolbar.setPadding(new Insets(10));

        exportCsvButton = new Button("Export Current Result CSV");
        exportCsvButton.setDisable(true);
        exportCsvButton.setOnAction(event -> exportCurrentResultToCsv(stage));

        exportPdfButton = new Button("Export Current Result PDF");
        exportPdfButton.setDisable(true);
        exportPdfButton.setOnAction(event -> exportCurrentResultToPdf(stage));

        toolbar.getChildren().addAll(exportCsvButton, exportPdfButton);
        return toolbar;
    }

    private void chooseResumeFile(Stage stage) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Resume File");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Resume Files", SUPPORTED_EXTENSIONS));
        File file = chooser.showOpenDialog(stage);
        if (file != null) {
            selectedFile = file;
            selectedFileLabel.setText(file.getName());
            analyzeButton.setDisable(false);
            resumeContentArea.clear();
            analysisOutputArea.clear();
            exportCsvButton.setDisable(true);
            exportPdfButton.setDisable(true);
        }
    }

    private void analyzeSelectedResume() {
        if (selectedFile == null) {
            return;
        }

        try {
            String text = ResumeParser.extractText(selectedFile.getAbsolutePath());
            resumeContentArea.setText(text);

            List<String> keywords = KeywordMatcher.loadDefaultKeywords();
            if (keywords.isEmpty()) {
                analysisOutputArea.setText("No keywords found in keywords.txt.");
                return;
            }

            KeywordMatcher.MatchResult result = KeywordMatcher.match(text, keywords);
            List<String> suggestions = FeedbackGenerator.generateSuggestions(text, result.missingKeywords);

            currentSummary = new AnalysisSummary(selectedFile.getName(), result.scorePercentage, result.missingKeywords, suggestions);
            analysisOutputArea.setText(formatAnalysis(result, suggestions));
            exportCsvButton.setDisable(false);
            exportPdfButton.setDisable(false);

            saveAnalysisResult(selectedFile.getName(), result.scorePercentage);
            refreshPastResults();
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to read resume", e);
            analysisOutputArea.setText("Failed to read resume: " + e.getMessage());
        }
    }

    private String formatAnalysis(KeywordMatcher.MatchResult result, List<String> suggestions) {
        StringBuilder builder = new StringBuilder();
        builder.append(String.format("Match percentage: %.2f%%%n", result.scorePercentage));
        builder.append(String.format("Matched keywords: %d%n", result.foundKeywords.size()));
        builder.append("\nMissing keywords:\n");
        if (result.missingKeywords.isEmpty()) {
            builder.append("None\n");
        } else {
            for (String missing : result.missingKeywords) {
                builder.append(" - ").append(missing).append("\n");
            }
        }
        builder.append("\nSuggestions:\n");
        for (String suggestion : suggestions) {
            builder.append(" - ").append(suggestion).append("\n");
        }
        return builder.toString();
    }

    private void saveAnalysisResult(String resumeName, double score) {
        try {
            DatabaseHandler handler = new DatabaseHandler();
            handler.saveResult(resumeName, score);
            LOGGER.info("Saved analysis result for " + resumeName);
        } catch (IOException | java.sql.SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to save analysis result", e);
            analysisOutputArea.appendText("\nFailed to save analysis result: " + e.getMessage());
        }
    }

    private void refreshPastResults() {
        try {
            DatabaseHandler handler = new DatabaseHandler();
            List<DatabaseHandler.AnalysisResult> results = handler.fetchAnalysisResults();
            ObservableList<DatabaseHandler.AnalysisResult> items = FXCollections.observableArrayList(results);
            pastResultsTable.setItems(items);
        } catch (IOException | java.sql.SQLException e) {
            LOGGER.log(Level.WARNING, "Unable to load past results", e);
            if (pastResultsTable != null) {
                pastResultsTable.setItems(FXCollections.observableArrayList());
            }
        }
    }

    private void showSettingsDialog(Stage owner) {
        try {
            Properties props = DatabaseHandler.loadConfigProperties();
            DialogWindow dialog = new DialogWindow(owner, props.getProperty("jdbc.url", ""), props.getProperty("db.user", ""), props.getProperty("db.password", ""));
            dialog.showAndWait();
            if (dialog.isSaved()) {
                DatabaseHandler.saveProperties(dialog.getUrl(), dialog.getUser(), dialog.getPassword());
                refreshPastResults();
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Unable to load DB settings", e);
            DialogWindow dialog = new DialogWindow(owner, "", "", "");
            dialog.showAndWait();
            if (dialog.isSaved()) {
                try {
                    DatabaseHandler.saveProperties(dialog.getUrl(), dialog.getUser(), dialog.getPassword());
                    refreshPastResults();
                } catch (IOException ex) {
                    LOGGER.log(Level.SEVERE, "Unable to save DB settings", ex);
                }
            }
        }
    }

    private void exportCurrentResultToCsv(Stage stage) {
        if (currentSummary == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Current Analysis to CSV");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            try {
                exportAnalysisToCsv(currentSummary, file);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to export CSV", e);
            }
        }
    }

    private void exportCurrentResultToPdf(Stage stage) {
        if (currentSummary == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Current Analysis to PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        File file = chooser.showSaveDialog(stage);
        if (file != null) {
            // Log exact path chosen by the user
            LOGGER.info("User selected PDF path: " + file.getAbsolutePath());
            System.out.println("User selected PDF path: " + file.getAbsolutePath());
            try {
                exportAnalysisToPdf(currentSummary, file);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to export PDF to " + file.getAbsolutePath(), e);
            }
        }
    }

    private void exportPastResultsToCsv() {
        try {
            DatabaseHandler handler = new DatabaseHandler();
            List<DatabaseHandler.AnalysisResult> results = handler.fetchAnalysisResults();
            File file = new File("past-analysis-results.csv");
            try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
                writer.write("id,resume_name,match_score,analysis_date\n");
                for (DatabaseHandler.AnalysisResult result : results) {
                    writer.write(String.format("%d,%s,%.2f,%s\n",
                            result.id,
                            escapeCsv(result.resumeName),
                            result.matchScore,
                            escapeCsv(result.analysisDate)));
                }
            }
            LOGGER.info("Exported past results to " + file.getAbsolutePath());
        } catch (IOException | java.sql.SQLException e) {
            LOGGER.log(Level.SEVERE, "Failed to export past results", e);
        }
    }

    private void exportPastResultsToPdf() {
        try {
            DatabaseHandler handler = new DatabaseHandler();
            List<DatabaseHandler.AnalysisResult> results = handler.fetchAnalysisResults();
            File file = new File("past-analysis-results.pdf");
            // Use OpenPDF to generate a simple table PDF
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, new java.io.FileOutputStream(file));
            doc.open();
            com.lowagie.text.Paragraph title = new com.lowagie.text.Paragraph("Past Analysis Results");
            doc.add(title);
            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(4);
            table.addCell("ID");
            table.addCell("Resume Name");
            table.addCell("Match %");
            table.addCell("Analysis Date");
            for (DatabaseHandler.AnalysisResult r : results) {
                table.addCell(String.valueOf(r.id));
                table.addCell(r.resumeName);
                table.addCell(String.format("%.2f", r.matchScore));
                table.addCell(r.analysisDate);
            }
            doc.add(table);
            doc.close();
            LOGGER.info("Exported past results PDF to " + file.getAbsolutePath());
        } catch (IOException | java.sql.SQLException | com.lowagie.text.DocumentException e) {
            LOGGER.log(java.util.logging.Level.SEVERE, "Failed to export past results to PDF", e);
        }
    }

    private static void exportAnalysisToCsv(AnalysisSummary summary, File file) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(file.toPath(), StandardCharsets.UTF_8)) {
            writer.write("Resume Name,Match Percentage,Missing Keywords,Suggestions\n");
            writer.write(String.format("%s,%.2f,%s,%s\n",
                    escapeCsv(summary.resumeName),
                    summary.matchScore,
                    escapeCsv(String.join("; ", summary.missingKeywords)),
                    escapeCsv(String.join("; ", summary.suggestions))));
        }
    }

    private static void exportAnalysisToPdf(AnalysisSummary summary, File file) throws IOException {
        com.lowagie.text.Document doc = new com.lowagie.text.Document();
        try (java.io.FileOutputStream fos = new java.io.FileOutputStream(file)) {
            com.lowagie.text.pdf.PdfWriter.getInstance(doc, fos);
            doc.open();

            com.lowagie.text.Font headerFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 18, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font titleFont = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 14, com.lowagie.text.Font.BOLD);
            com.lowagie.text.Font normal = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12);
            com.lowagie.text.Font bold = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 12, com.lowagie.text.Font.BOLD);

            // Header
            com.lowagie.text.Paragraph header = new com.lowagie.text.Paragraph("AI Powered Resume Analyzer", headerFont);
            header.setSpacingAfter(10f);
            doc.add(header);

            // Candidate and score
            com.lowagie.text.Paragraph candidate = new com.lowagie.text.Paragraph("Candidate: " + (summary.resumeName == null ? "Unknown" : summary.resumeName), titleFont);
            candidate.setSpacingAfter(6f);
            doc.add(candidate);

            com.lowagie.text.Paragraph score = new com.lowagie.text.Paragraph(String.format("Overall Match: %.2f%%", summary.matchScore), bold);
            score.setSpacingAfter(12f);
            doc.add(score);

            // Keyword table: Keyword | Found | Suggestion
            com.lowagie.text.Paragraph tableTitle = new com.lowagie.text.Paragraph("Keyword Results", titleFont);
            tableTitle.setSpacingAfter(6f);
            doc.add(tableTitle);

            com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(6f);
            table.setSpacingAfter(12f);

            com.lowagie.text.pdf.PdfPCell h1 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Keyword", bold));
            com.lowagie.text.pdf.PdfPCell h2 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Found", bold));
            com.lowagie.text.pdf.PdfPCell h3 = new com.lowagie.text.pdf.PdfPCell(new com.lowagie.text.Phrase("Suggestion", bold));
            table.addCell(h1);
            table.addCell(h2);
            table.addCell(h3);

            // Load default keywords and populate rows. If keywords cannot be loaded, fall back to missingKeywords list.
            List<String> keywords = new ArrayList<>();
            try {
                keywords = KeywordMatcher.loadDefaultKeywords();
            } catch (IOException e) {
                // fallback: use missing keywords and suggestions
            }

            if (keywords.isEmpty()) {
                // derive from missing keywords (mark as not found)
                for (String missing : summary.missingKeywords) {
                    table.addCell(new com.lowagie.text.Phrase(missing, normal));
                    table.addCell(new com.lowagie.text.Phrase("No", normal));
                    table.addCell(new com.lowagie.text.Phrase("Consider adding: " + missing, normal));
                }
            } else {
                for (String kw : keywords) {
                    boolean found = !summary.missingKeywords.contains(kw);
                    table.addCell(new com.lowagie.text.Phrase(kw, normal));
                    table.addCell(new com.lowagie.text.Phrase(found ? "Yes" : "No", normal));
                    String suggestion = found ? "" : "Consider adding: " + kw;
                    table.addCell(new com.lowagie.text.Phrase(suggestion, normal));
                }
            }

            doc.add(table);

            // Summary / Recommendations
            com.lowagie.text.Paragraph summaryTitle = new com.lowagie.text.Paragraph("Recommendations", titleFont);
            summaryTitle.setSpacingAfter(6f);
            doc.add(summaryTitle);

            if (summary.suggestions == null || summary.suggestions.isEmpty()) {
                doc.add(new com.lowagie.text.Paragraph("No recommendations.", normal));
            } else {
                com.lowagie.text.List recList = new com.lowagie.text.List(com.lowagie.text.List.ORDERED);
                for (String s : summary.suggestions) {
                    recList.add(new com.lowagie.text.ListItem(new com.lowagie.text.Phrase(s, normal)));
                }
                doc.add(recList);
            }

            doc.close();

            String msg = "PDF exported successfully! Saved at: " + file.getAbsolutePath();
            LOGGER.info(msg);
            System.out.println(msg);
        } catch (com.lowagie.text.DocumentException e) {
            throw new IOException("Failed to generate PDF", e);
        }
    }

    private static String escapeCsv(String value) {
        if (value.contains(",") || value.contains("\n") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private static class AnalysisSummary {
        final String resumeName;
        final double matchScore;
        final List<String> missingKeywords;
        final List<String> suggestions;

        AnalysisSummary(String resumeName, double matchScore, List<String> missingKeywords, List<String> suggestions) {
            this.resumeName = resumeName;
            this.matchScore = matchScore;
            this.missingKeywords = new ArrayList<>(missingKeywords);
            this.suggestions = new ArrayList<>(suggestions);
        }
    }

    private static class DialogWindow extends Stage {
        private final TextField urlField;
        private final TextField userField;
        private final TextField passwordField;
        private boolean saved;

        DialogWindow(Stage owner, String url, String user, String password) {
            initOwner(owner);
            initModality(Modality.APPLICATION_MODAL);
            setTitle("Database Settings");

            GridPane grid = new GridPane();
            grid.setPadding(new Insets(15));
            grid.setVgap(10);
            grid.setHgap(10);

            Label urlLabel = new Label("JDBC URL:");
            urlField = new TextField(url);
            grid.add(urlLabel, 0, 0);
            grid.add(urlField, 1, 0);

            Label userLabel = new Label("DB Username:");
            userField = new TextField(user);
            grid.add(userLabel, 0, 1);
            grid.add(userField, 1, 1);

            Label passwordLabel = new Label("DB Password:");
            passwordField = new TextField(password);
            grid.add(passwordLabel, 0, 2);
            grid.add(passwordField, 1, 2);

            HBox buttons = new HBox(10);
            Button saveButton = new Button("Save");
            Button cancelButton = new Button("Cancel");
            buttons.getChildren().addAll(saveButton, cancelButton);
            grid.add(buttons, 1, 3);

            saveButton.setOnAction(e -> {
                saved = true;
                close();
            });
            cancelButton.setOnAction(e -> close());

            Scene scene = new Scene(grid, 650, 220);
            setScene(scene);
        }

        boolean isSaved() {
            return saved;
        }

        String getUrl() {
            return urlField.getText().trim();
        }

        String getUser() {
            return userField.getText().trim();
        }

        String getPassword() {
            return passwordField.getText().trim();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
