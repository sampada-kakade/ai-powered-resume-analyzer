# AI Powered Resume Analyzer

An intelligent JavaFX application that analyzes resumes against job requirements and provides insights such as keyword matches, missing skills, and recommendations. Includes export options to **CSV** and **PDF** with a professional layout.

-------

## 🚀 Features
- Upload resumes (`.txt` or `.pdf`)
- Run AI‑powered analysis
- Export results to **CSV** and **PDF**
- PDF includes:
  - Header with app name
  - Candidate name
  - Overall match score
  - Keyword table (Keyword | Found | Suggestion)
  - Recommendations section

---

## 🛠️ Build & Run

### Build the app module
```bash
mvn -pl app -DskipTests package


----
# ResumeAnalyzer

Simple Java CLI and JavaFX GUI tool to analyze resumes (TXT/PDF) against a set of keywords, provide feedback, and store results in MySQL.

Requirements
- Java 11+
- Maven
- MySQL database

Build

```bash
mvn package
```

Test

```bash
mvn test
```

Run

```bash
java -cp target/ResumeAnalyzer-1.0-SNAPSHOT.jar;target/lib/* com.resumeanalyzer.Main
```

Or run directly through Maven without manually specifying the classpath:

```bash
mvn exec:java
```

Use the JavaFX GUI with:

```bash
mvn javafx:run
```

API

The project includes a minimal REST API module under `api/`. To run the API (Spring Boot):

```bash
cd api
mvn spring-boot:run
```

Endpoints:
- `POST /analyze` (text/plain) — return JSON analysis for provided resume text
- `GET /results` — returns past analysis rows from the database

Docker

Build the Docker image (multi-stage build):

```bash
docker build -t resumeanalyzer:latest .
docker run --rm -p 8080:8080 resumeanalyzer:latest
```

GUI Screenshots

![GUI main window](docs/screenshots/gui-main.png)
![Analysis results](docs/screenshots/gui-results.png)<img width="1244" height="930" alt="Screenshot 2026-06-05 095700" src="https://github.com/user-attachments/assets/f04ec8c1-de71-40b3-9479-8e89db310a4b" />


If your dependencies are not on disk, use Maven to package or run the application directly with the exec plugin.

Usage

1. Create `src/main/resources/db.properties` with MySQL settings:

```properties
jdbc.url=jdbc:mysql://localhost:3306/resume_db
db.user=your_user
db.password=your_password
```

2. Update `src/main/resources/keywords.txt` to match the job-description keywords you care about.

3. Run the program and choose:
   - `1` to analyze a resume
   - `2` to view past analysis results
   - `3` to exit

4. For a quick test, use the provided `sample_resume.txt` file at the project root.

Example

```bash
java -cp target/ResumeAnalyzer-1.0-SNAPSHOT.jar;target/lib/* com.resumeanalyzer.Main
```

Then choose option `1` and enter the path to `sample_resume.txt`.

Database

- Create a database (e.g., `resume_db`) and grant a user privileges.
- Make sure `db.properties` contains the correct connection settings.
- The application writes results to the `analysis_results` table automatically.

##Notes
- Keywords are loaded from `src/main/resources/keywords.txt`.
- Logging is enabled via `java.util.logging` for parsing, analysis, and database save events.
- JUnit tests are included for keyword matching.
