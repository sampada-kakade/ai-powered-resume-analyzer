# Multi-stage build: build with Maven, runtime with slim JRE
FROM maven:3.8.8-openjdk-17 as build
WORKDIR /workspace
COPY pom.xml ./
COPY src ./src
RUN mvn -q -DskipTests package

FROM openjdk:17-jdk-slim
RUN apt-get update && apt-get install -y --no-install-recommends \
    mariadb-client \
 && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /workspace/target/ResumeAnalyzer-1.0-SNAPSHOT.jar ./app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app/app.jar"]
