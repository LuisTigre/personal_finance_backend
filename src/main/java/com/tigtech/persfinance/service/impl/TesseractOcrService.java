package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.service.OcrService;
import com.tigtech.persfinance.service.dto.OcrResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class TesseractOcrService implements OcrService {

    @Value("${app.ocr.tesseractPath:tesseract}")
    private String tesseractPath;

    @Value("${app.ocr.language:pol+eng}")
    private String language;

    @Value("${app.ocr.psm:6}")
    private String psm;

    @Autowired
    private OcrDumpService dumpService;

    @Override
    public OcrResult processUpload(MultipartFile file) throws IOException {
        String requestId = dumpService.initRequest(file.getOriginalFilename());
        try {
            OcrResult result = performOcr(file, 0, requestId);
            
            // Dump meta
            if (requestId != null) {
                dumpService.dumpCombinedText(requestId, result.getFullText());
                dumpService.dumpMeta(requestId, java.util.Map.of(
                    "originalFilename", file.getOriginalFilename() != null ? file.getOriginalFilename() : "unknown",
                    "contentType", file.getContentType() != null ? file.getContentType() : "unknown",
                    "pageCount", "1",
                    "language", language,
                    "psm", psm
                ));
            }
            return result;
        } finally {
            dumpService.cleanup(requestId);
        }
    }

    @Override
    public String extractText(MultipartFile file) throws IOException {
        return processUpload(file).getFullText();
    }
    
    // Public variant to be called by PDF service wrapper if we want per-page requestId (but ideally we share it)
    public OcrResult performOcrForPage(MultipartFile file, int pageIndex, String requestId) throws IOException {
        return performOcr(file, pageIndex, requestId);
    }
    
    // Internal helper
    private OcrResult performOcr(MultipartFile file, int pageIndex, String requestId) throws IOException {
        Path tempInput = Files.createTempFile("ocr_input_", ".tmp");
        // Tesseract adds extension to output, we use "stdout" so no file output
        
        try {
            file.transferTo(tempInput.toFile());

            // Command: tesseract <input> stdout -l <lang> --psm <psm> tsv
            ProcessBuilder pb = new ProcessBuilder(
                    tesseractPath,
                    tempInput.toString(),
                    "stdout",
                    "-l", language,
                    "--psm", psm,
                    "tsv"
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read process output
            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroy();
                throw new IOException("OCR process timed out");
            }

            if (process.exitValue() != 0) {
                 log.error("Tesseract Output: {}", output);
                 throw new IOException("OCR process failed with exit code " + process.exitValue());
            }

            // Dump raw output (TSV)
            if (requestId != null) {
                 dumpService.dumpPageTsv(requestId, pageIndex, output.toString());
            }

            OcrResult result = parseTsvOutput(output.toString(), pageIndex);
            
            // Dump parsed text
            if (requestId != null) {
                dumpService.dumpPageText(requestId, pageIndex, result.getFullText());
            }
            
            return result;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("OCR process interrupted", e);
        } finally {
            try {
                Files.deleteIfExists(tempInput);
            } catch (IOException e) {
                log.warn("Failed to clean up temp files", e);
            }
        }
    }

    private OcrResult parseTsvOutput(String tsvContent, int pageIndex) {
        List<OcrResult.OcrToken> tokens = new ArrayList<>();
        String[] lines = tsvContent.split("\\n");
        
        int headerIndex = -1;
        // Find header row: level page_num block_num ...
        for (int i = 0; i < Math.min(lines.length, 20); i++) { // Check first 20 lines for header
            if (lines[i].contains("level") && lines[i].contains("page_num") && lines[i].contains("text")) {
                headerIndex = i;
                break;
            }
        }

        if (headerIndex == -1) {
             log.warn("Could not find TSV header in Tesseract output for page {}. Content preview: {}", 
                     pageIndex, tsvContent.substring(0, Math.min(tsvContent.length(), 200)));
             // Fallback: try to regular text parse or just return empty?
             // If we really can't parse TSV, we return empty info but warn.
             return OcrResult.builder()
                     .fullText("")
                     .lines(new ArrayList<>())
                     .pages(new ArrayList<>())
                     .build();
        }
        
        // Skip header row and parse following lines
        for (int i = headerIndex + 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) continue;
            
            String[] parts = line.split("\\t");
            if (parts.length < 12) continue; // Basic validation
            
            try {
                // Tesseract 5.x TSV can output float confidence (e.g. 96.1234)
                double confDouble = Double.parseDouble(parts[10]);
                int conf = (int) confDouble; 
                String text = parts[11].trim();
                
                if (conf > 40 && !text.isEmpty()) {
                    tokens.add(OcrResult.OcrToken.builder()
                            .left(Integer.parseInt(parts[6]))
                            .top(Integer.parseInt(parts[7]))
                            .width(Integer.parseInt(parts[8]))
                            .height(Integer.parseInt(parts[9]))
                            .confidence(conf)
                            .text(text)
                            .build());
                }
            } catch (NumberFormatException ignored) {}
        }
        
        // Reconstruct lines from tokens
        List<OcrResult.OcrLine> reconstructedLines = reconstructLines(tokens, pageIndex);
        
        String fullText = reconstructedLines.stream()
                .map(OcrResult.OcrLine::getText)
                .collect(Collectors.joining("\n"));

        return OcrResult.builder()
                .fullText(fullText)
                .lines(reconstructedLines)
                .pages(List.of(OcrResult.OcrPageResult.builder()
                        .pageIndex(pageIndex)
                        .text(fullText)
                        .build()))
                .build();
    }

    private List<OcrResult.OcrLine> reconstructLines(List<OcrResult.OcrToken> tokens, int pageIndex) {
        List<OcrResult.OcrLine> lines = new ArrayList<>();
        if (tokens.isEmpty()) return lines;

        // Group tokens by Y coordinate (row tolerance e.g. 10px)
        // Sort by Top first
        tokens.sort(Comparator.comparingInt(OcrResult.OcrToken::getTop));
        
        List<OcrResult.OcrToken> currentLineTokens = new ArrayList<>();
        OcrResult.OcrToken firstInLine = tokens.get(0);
        currentLineTokens.add(firstInLine);
        
        int rowTolerance = 12; // px
        
        for (int i = 1; i < tokens.size(); i++) {
            OcrResult.OcrToken token = tokens.get(i);
            // Check vertical alignment with the "average" or first token of current line
            // Simple approach: compare with first token's top
            if (Math.abs(token.getTop() - firstInLine.getTop()) <= rowTolerance) {
                currentLineTokens.add(token);
            } else {
                // Finalize current line
                lines.add(buildLine(currentLineTokens, pageIndex));
                
                // Start new line
                currentLineTokens = new ArrayList<>();
                currentLineTokens.add(token);
                firstInLine = token;
            }
        }
        // Add last line
        if (!currentLineTokens.isEmpty()) {
            lines.add(buildLine(currentLineTokens, pageIndex));
        }
        
        return lines;
    }
    
    private OcrResult.OcrLine buildLine(List<OcrResult.OcrToken> tokens, int pageIndex) {
        // Sort by X (Left)
        tokens.sort(Comparator.comparingInt(OcrResult.OcrToken::getLeft));
        
        String text = tokens.stream()
                .map(OcrResult.OcrToken::getText)
                .collect(Collectors.joining(" "));
                
        return OcrResult.OcrLine.builder()
                .text(text)
                .tokens(tokens)
                .pageIndex(pageIndex)
                .build();
    }
}
