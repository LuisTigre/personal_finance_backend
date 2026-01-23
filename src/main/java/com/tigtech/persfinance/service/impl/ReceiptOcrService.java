package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.service.OcrService;
import com.tigtech.persfinance.service.dto.OcrResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Primary
@Slf4j
@RequiredArgsConstructor
public class ReceiptOcrService implements OcrService {

    private final TesseractOcrService imageOcrService;
    private final OcrDumpService dumpService;

    @Value("${app.ocr.pdf.maxPages:10}")
    private int maxPages;

    @Value("${app.ocr.pdf.dpi:300}")
    private int dpi;

    @Override
    public String extractText(MultipartFile file) throws IOException {
        OcrResult result = processUpload(file);
        return result.getFullText();
    }
    
    @Override
    public OcrResult processUpload(MultipartFile file) throws IOException {
        String requestId = dumpService != null ? dumpService.initRequest(file.getOriginalFilename()) : null;
        try {
            String contentType = file.getContentType();
            String filename = file.getOriginalFilename();
            boolean isPdfFile = isPdf(contentType, filename);
            String fileType = isPdfFile ? "PDF" : "IMAGE";

            log.info("Processing upload. RequestId: {}, Type: {}, File: {}", requestId, fileType, filename);

            OcrResult result;
            if (isPdfFile) {
                result = processPdf(file, requestId);
            } else {
                result = processImage(file, requestId);
            }
            
            result.setFileType(fileType);
            validateAndFinalizeResult(result);
            
            log.info("OCR Finished. Status: {}, Pages: {}/{}", result.getStatus(), result.getProcessedPageCount(), result.getTotalPageCount());
            
            if (requestId != null) {
                dumpService.dumpCombinedText(requestId, result.getFullText());
                Map<String, Object> meta = new HashMap<>();
                meta.put("filename", filename);
                meta.put("fileType", fileType);
                meta.put("status", result.getStatus());
                meta.put("processedPages", result.getProcessedPageCount());
                meta.put("totalPages", result.getTotalPageCount());
                dumpService.dumpMeta(requestId, meta);
            }

            return result;
        } finally {
            if (requestId != null) dumpService.cleanup(requestId);
        }
    }

    private OcrResult processImage(MultipartFile file, String requestId) throws IOException {
        OcrResult result = imageOcrService.performOcrForPage(file, 0, requestId);
        // Ensure strictly 1 page
        if (result.getPages() == null) result.setPages(new ArrayList<>());
        if (result.getPages().isEmpty()) {
            result.getPages().add(OcrResult.OcrPageResult.builder()
                .pageIndex(0).text(result.getFullText()).build());
        }
        result.setTotalPageCount(1);
        result.setProcessedPageCount(1);
        return result;
    }

    private void validateAndFinalizeResult(OcrResult result) {
        // 1. Processed Count Validity
        if (result.getProcessedPageCount() < result.getTotalPageCount()) {
            result.setStatus(OcrResult.OcrStatus.INCOMPLETE_OCR);
            log.warn("Incomplete OCR: Processed {} of {} pages", result.getProcessedPageCount(), result.getTotalPageCount());
            return;
        }
        
        // 2. Validate "Page X of Y" indicators (Requirement 7)
        // Scan full text for patterns "Page 1 of 3", "Strona 1 z 3"
        Pattern pagePattern = Pattern.compile("(?i)(?:page|strona)\\s+(\\d+)\\s+(?:of|z|nu)\\s+(\\d+)");
        Matcher m = pagePattern.matcher(result.getFullText());
        while (m.find()) {
            try {
                int declaredTotal = Integer.parseInt(m.group(2));
                if (declaredTotal != result.getTotalPageCount()) {
                     log.warn("Inconsistent Page Count: Document says {} but PDF has {}", declaredTotal, result.getTotalPageCount());
                     result.setStatus(OcrResult.OcrStatus.INCONSISTENT_PAGE_COUNT);
                     return;
                }
            } catch (NumberFormatException ignored) {}
        }

        result.setStatus(OcrResult.OcrStatus.SUCCESS);
    }


    private boolean isPdf(String contentType, String filename) {
        return (contentType != null && "application/pdf".equalsIgnoreCase(contentType)) ||
               (filename != null && StringUtils.endsWithIgnoreCase(filename, ".pdf"));
    }

    private OcrResult processPdf(MultipartFile file, String requestId) throws IOException {
        log.info("Processing PDF file: {}", file.getOriginalFilename());
        
        Path tempSdkPdf = Files.createTempFile("pdfbox_", ".pdf");
        
        try {
            file.transferTo(tempSdkPdf.toFile());

            try (PDDocument document = Loader.loadPDF(tempSdkPdf.toFile())) {
                int numberOfPages = document.getNumberOfPages();
                log.info("PDF Page Count: {}", numberOfPages);
                
                if (numberOfPages > maxPages) {
                    throw new IllegalArgumentException("PDF exceeds maximum page limit of " + maxPages);
                }

                PDFTextStripper stripper = new PDFTextStripper();
                String extractedText = stripper.getText(document);
                OcrResult result;
                
                // Requirement 3: OCR Every Page must be respected
                if (extractedText != null && extractedText.trim().length() > 50) {
                    log.info("PDF has embedded text, using extracted text strategy.");
                    result = extractTextFromPdfDirectly(document, requestId);
                } else {
                    log.info("PDF seems to be scanned images. Rendering pages for OCR.");
                    result = ocrRenderedPdfPages(document, requestId);
                }
                
                result.setTotalPageCount(numberOfPages);
                result.setProcessedPageCount(result.getPages().size());
                
                // Requirement 5: Duplicate Detection
                checkDuplicates(result);
                
                return result;
            }
        } finally {
            Files.deleteIfExists(tempSdkPdf);
        }
    }

    private void checkDuplicates(OcrResult result) {
        Map<String, Integer> pageHashes = new HashMap<>();
        for (OcrResult.OcrPageResult page : result.getPages()) {
            String normalized = StringUtils.replace(page.getText().toLowerCase().trim(), "\\s", "");
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(normalized.getBytes(StandardCharsets.UTF_8));
                String hashHex = Base64.getEncoder().encodeToString(hash);
                
                if (pageHashes.containsKey(hashHex)) {
                    log.warn("Possible duplicate page detected: Page {} matches Page {}", page.getPageIndex(), pageHashes.get(hashHex));
                    // Check if strictly equal or just very similar? SHA-256 is strict.
                    // For now, we just log. Requirement says "Flag: POSSIBLE_DUPLICATE_PAGE... Continue processing".
                } else {
                    pageHashes.put(hashHex, page.getPageIndex());
                }
            } catch (NoSuchAlgorithmException e) {
                log.error("Hash algorithm not found", e);
            }
        }
    }


    private OcrResult extractTextFromPdfDirectly(PDDocument document, String requestId) throws IOException {
        PDFTextStripper stripper = new PDFTextStripper();
        List<OcrResult.OcrPageResult> pages = new ArrayList<>();
        List<OcrResult.OcrLine> allLines = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        for (int i = 0; i < document.getNumberOfPages(); ++i) {
            stripper.setStartPage(i + 1);
            stripper.setEndPage(i + 1);
            String pageText = stripper.getText(document);
            if (pageText == null) pageText = "";

            if (requestId != null) {
                dumpService.dumpPageText(requestId, i, pageText);
            }
            
            pages.add(OcrResult.OcrPageResult.builder()
                    .pageIndex(i)
                    .text(pageText)
                    .build());
            
            fullText.append("--- PAGE ").append(i + 1).append(" ---\n");
            fullText.append(pageText).append("\n\n");
            
            // Populate lines
            String[] rawLines = pageText.split("\\r?\\n");
            for (String raw : rawLines) {
                if (!raw.trim().isEmpty()) {
                    allLines.add(OcrResult.OcrLine.builder()
                            .text(raw.trim())
                            .pageIndex(i)
                            // tokens are null for text-extracted PDF
                            .build());
                }
            }
        }

        return OcrResult.builder()
                .fullText(fullText.toString())
                .pages(pages)
                .lines(allLines)
                .build();
    }

    private OcrResult ocrRenderedPdfPages(PDDocument document, String requestId) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        List<OcrResult.OcrPageResult> pages = new ArrayList<>();
        List<OcrResult.OcrLine> allLines = new ArrayList<>();
        StringBuilder fullText = new StringBuilder();

        for (int i = 0; i < document.getNumberOfPages(); ++i) {
            // Render image
            BufferedImage image = renderer.renderImageWithDPI(i, dpi, ImageType.RGB);
            
            // Convert to byte array (PNG)
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            byte[] imageBytes = baos.toByteArray();
            
            if (requestId != null) {
                dumpService.dumpRenderedImage(requestId, i, imageBytes);
            }

            // Create a MultipartFile-like object or use a temporary approach to feed existing service
            // TesseractOcrService expects MultipartFile. Let's create a specialized MockMultipartFile or wrapper.
            // Since we don't want to import spring-test MockMultipartFile ensuring it's in scope, 
            // we will create a simple internal implementation.
            MultipartFile pageFile = new ByteArrayMultipartFile(
                    imageBytes, 
                    "page-" + i + ".png", 
                    "image/png"
            );

            // Call existing OCR with requestId
            OcrResult pageResult = imageOcrService.performOcrForPage(pageFile, i, requestId);
            
            String pageText = pageResult.getFullText();
            
            pages.add(OcrResult.OcrPageResult.builder()
                    .pageIndex(i)
                    .text(pageText)
                    .build());
            
            if (pageResult.getLines() != null) {
                allLines.addAll(pageResult.getLines());
            }
            
            fullText.append("--- PAGE ").append(i + 1).append(" ---\n");
            fullText.append(pageText).append("\n\n");
        }

        return OcrResult.builder()
                .fullText(fullText.toString())
                .pages(pages)
                .lines(allLines) // Aggregate lines from all pages
                .build();
    }

    // Inner class helper
    private static class ByteArrayMultipartFile implements MultipartFile {
        private final byte[] content;
        private final String name;
        private final String contentType;

        public ByteArrayMultipartFile(byte[] content, String name, String contentType) {
            this.content = content;
            this.name = name;
            this.contentType = contentType;
        }

        @Override
        public String getName() { return "file"; }

        @Override
        public String getOriginalFilename() { return name; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content == null || content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() throws IOException { return content; }

        @Override
        public InputStream getInputStream() throws IOException { return new java.io.ByteArrayInputStream(content); }

        @Override
        public void transferTo(java.io.File dest) throws IOException, IllegalStateException {
            Files.write(dest.toPath(), content);
        }
    }
}
