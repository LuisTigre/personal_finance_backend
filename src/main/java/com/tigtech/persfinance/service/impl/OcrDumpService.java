package com.tigtech.persfinance.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class OcrDumpService {

    @Value("${app.ocr.debugDumpEnabled:false}")
    private boolean debugDumpEnabled;

    @Value("${app.ocr.debugDumpDir:./ocr-dumps}")
    private String debugDumpDir;
    
    // Store request-specific paths mapping
    private final Map<String, Path> requestPaths = new ConcurrentHashMap<>();

    // Initialize Request
    public String initRequest(String filename) {
        if (!debugDumpEnabled) return null;
        
        String requestId = UUID.randomUUID().toString().substring(0, 8);
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String folderName = timestamp + "_" + requestId;
        
        try {
            Path dumpPath = Path.of(debugDumpDir, folderName);
            Files.createDirectories(dumpPath);
            requestPaths.put(requestId, dumpPath);
            log.info("OCR debug dump enabled for request {}. Output: {}", requestId, dumpPath);
            return requestId;
        } catch (IOException e) {
            log.warn("Failed to create OCR dump directory: {}", e.getMessage());
            return null; // Don't fail the request, just skip dumping
        }
    }

    public void dumpPageText(String requestId, int pageIndex, String text) {
        writeDump(requestId, "page_" + pageIndex + ".txt", text);
    }

    public void dumpPageTsv(String requestId, int pageIndex, String tsv) {
        writeDump(requestId, "page_" + pageIndex + ".tsv", tsv);
    }

    public void dumpCombinedText(String requestId, String combined) {
        writeDump(requestId, "combined.txt", combined);
    }

    public void dumpRenderedImage(String requestId, int pageIndex, byte[] imageBytes) {
        if (requestId == null || !debugDumpEnabled) return;

        Path path = requestPaths.get(requestId);
        if (path == null) return;

        try {
            Path file = path.resolve("page_" + pageIndex + ".png");
            Files.write(file, imageBytes);
        } catch (IOException e) {
            log.warn("Failed to dump OCR image: {}", e.getMessage());
        }
    }

    public void dumpMeta(String requestId, Map<String, Object> meta) {
        if (requestId == null || !debugDumpEnabled) return;
        
        // Simple manual JSON construction to avoid adding new Jackson dependency inside service if strictly not needed, 
        // but typically Jackson is available in Spring Boot.
        StringBuilder json = new StringBuilder("{\n");
        meta.forEach((k, v) -> json.append("  \"").append(k).append("\": \"").append(v).append("\",\n"));
        // remove last comma
        if (json.length() > 2) json.setLength(json.length() - 2);
        json.append("\n}");

        writeDump(requestId, "meta.json", json.toString());
    }

    private void writeDump(String requestId, String filename, String content) {
        if (requestId == null || !debugDumpEnabled || content == null) return;

        Path path = requestPaths.get(requestId);
        if (path == null) return;

        try {
            Files.writeString(path.resolve(filename), content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.warn("Failed to dump OCR file {}: {}", filename, e.getMessage());
        }
    }
    
    // Cleanup if needed, though map entries are smallstrings
    public void cleanup(String requestId) {
         if (requestId != null) requestPaths.remove(requestId);
    }
}
