package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.service.OcrService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class TesseractOcrService implements OcrService {

    @Value("${app.ocr.tesseractPath:tesseract}")
    private String tesseractPath;

    @Value("${app.ocr.language:pol}")
    private String language;

    @Override
    public String extractText(MultipartFile file) throws IOException {
        Path tempInput = Files.createTempFile("ocr_input_", ".tmp");
        Path tempOutput = Files.createTempFile("ocr_output_", ""); // Tesseract adds .txt extension

        try {
            file.transferTo(tempInput.toFile());

            ProcessBuilder pb = new ProcessBuilder(
                    tesseractPath,
                    tempInput.toString(),
                    tempOutput.toString(),
                    "-l", language
            );

            pb.redirectErrorStream(true);
            Process process = pb.start();

            // Read process output for logging errors
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("Tesseract: {}", line);
                }
            }

            if (!process.waitFor(30, TimeUnit.SECONDS)) {
                process.destroy();
                throw new IOException("OCR process timed out");
            }

            if (process.exitValue() != 0) {
                throw new IOException("OCR process failed with exit code " + process.exitValue());
            }
            
            // Tesseract appends .txt to the output filename
            Path actualOutputFile = Path.of(tempOutput.toString() + ".txt");
            if (Files.exists(actualOutputFile)) {
                return Files.readString(actualOutputFile);
            } else {
                 // Fallback if tesseract didn't add extension or something odd happened
                 log.warn("Expected output file {} not found", actualOutputFile);
                 return "";
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("OCR process interrupted", e);
        } finally {
            try {
                Files.deleteIfExists(tempInput);
                Files.deleteIfExists(tempOutput);
                Files.deleteIfExists(Path.of(tempOutput.toString() + ".txt"));
            } catch (IOException e) {
                log.warn("Failed to clean up temp files", e);
            }
        }
    }
}
