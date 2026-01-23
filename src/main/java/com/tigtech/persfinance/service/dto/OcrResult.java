package com.tigtech.persfinance.service.dto;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class OcrResult {
    private String fullText;
    private List<OcrPageResult> pages;
    private List<OcrLine> lines; // Reconstructed lines from all pages
    
    // Metadata & Validation
    private String fileType;        // "PDF" or "IMAGE"
    private Integer totalPageCount; // From PDF metadata or 1 for image
    private int processedPageCount; // Actually processed pages
    private OcrStatus status;       // Validation status
    
    public enum OcrStatus {
        SUCCESS,
        INCOMPLETE_OCR,         // processed < total
        FAILED_OCR,             // Critically failed
        INCONSISTENT_PAGE_COUNT // "Page 1 of 3" found but PDF has 2 pages
    }

    @Data
    @Builder
    public static class OcrPageResult {
        private int pageIndex;
        private String text;
    }

    @Data
    @Builder
    public static class OcrLine {
        private String text;
        private List<OcrToken> tokens;
        private int pageIndex;
    }

    @Data
    @Builder
    public static class OcrToken {
        private String text;
        private int left;
        private int top;
        private int width;
        private int height;
        private int confidence;
    }
}
