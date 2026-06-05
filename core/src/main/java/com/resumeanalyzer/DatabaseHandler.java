package com.resumeanalyzer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class DatabaseHandler {
    private static final Logger LOGGER = Logger.getLogger(DatabaseHandler.class.getName());
    private static final String CONFIG_FILE = "db.properties";

    private final String url;
    private final String user;
    private final String password;

    public DatabaseHandler() throws IOException, SQLException {
        Properties props = loadConfigProperties();
        this.url = getRequired(props, "jdbc.url");
        this.user = getRequired(props, "db.user");
        this.password = getRequired(props, "db.password");
        LOGGER.info("Loaded database configuration from " + CONFIG_FILE);
        ensureTable();
    }

    public static Properties loadConfigProperties() throws IOException {
        Properties props = new Properties();
        Path externalConfig = Path.of(CONFIG_FILE);
        if (Files.exists(externalConfig)) {
            try (InputStream input = Files.newInputStream(externalConfig)) {
                props.load(input);
                LOGGER.info("Loaded external database configuration from " + externalConfig.toAbsolutePath());
                return props;
            }
        }

        try (InputStream input = Thread.currentThread().getContextClassLoader().getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new IOException(CONFIG_FILE + " not found in classpath or project directory");
            }
            props.load(input);
            LOGGER.info("Loaded packaged database configuration from classpath");
        }
        return props;
    }

    public static void saveProperties(String url, String user, String password) throws IOException {
        Properties props = new Properties();
        props.setProperty("jdbc.url", url);
        props.setProperty("db.user", user);
        props.setProperty("db.password", password);
        Path externalConfig = Path.of(CONFIG_FILE);
        try (OutputStream output = Files.newOutputStream(externalConfig)) {
            props.store(output, "ResumeAnalyzer database connection settings");
        }
        Logger.getLogger(DatabaseHandler.class.getName()).info("Saved database settings to " + externalConfig.toAbsolutePath());
    }

    private String getRequired(Properties props, String key) throws IOException {
        String value = props.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            throw new IOException("Missing required property: " + key + " in " + CONFIG_FILE);
        }
        return value.trim();
    }

    private Connection connect() throws SQLException {
        return DriverManager.getConnection(url, user, password);
    }

    private void ensureTable() throws SQLException {
        String sql = "CREATE TABLE IF NOT EXISTS analysis_results (" +
                "id INT AUTO_INCREMENT PRIMARY KEY, " +
                "resume_name VARCHAR(255), " +
                "match_score DOUBLE, " +
                "analysis_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.execute();
            LOGGER.info("Ensured analysis_results table exists");
        }
    }

    public void saveResult(String resumeName, double score) throws SQLException {
        String sql = "INSERT INTO analysis_results (resume_name, match_score) VALUES (?, ?)";
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, resumeName);
            ps.setDouble(2, score);
            ps.executeUpdate();
            LOGGER.info("Saved analysis result for resume: " + resumeName);
        }
    }

    public List<AnalysisResult> fetchAnalysisResults() throws SQLException {
        String sql = "SELECT id, resume_name, match_score, analysis_date FROM analysis_results ORDER BY analysis_date DESC";
        List<AnalysisResult> results = new ArrayList<>();
        try (Connection c = connect(); PreparedStatement ps = c.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                results.add(new AnalysisResult(
                        rs.getInt("id"),
                        rs.getString("resume_name"),
                        rs.getDouble("match_score"),
                        Objects.toString(rs.getTimestamp("analysis_date"), "")));
            }
        }
        return results;
    }

    public static class AnalysisResult {
        public final int id;
        public final String resumeName;
        public final double matchScore;
        public final String analysisDate;

        public AnalysisResult(int id, String resumeName, double matchScore, String analysisDate) {
            this.id = id;
            this.resumeName = resumeName;
            this.matchScore = matchScore;
            this.analysisDate = analysisDate;
        }
    }
}
