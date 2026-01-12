# Multi-stage Dockerfile: build with Maven, run with JRE
FROM maven:3.9.4-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml mvnw .mvn/ ./
COPY src ./src
RUN mvn -B -DskipTests package

FROM eclipse-temurin:17-jre
# Install Tesseract for OCR features
RUN apt-get update && \
    apt-get install -y tesseract-ocr tesseract-ocr-pol && \
    rm -rf /var/lib/apt/lists/*

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
