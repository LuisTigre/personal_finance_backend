package com.tigtech.persfinance.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReceiptOcrDraftResponse {
    private String fullText; // Alias/Same as ocrText but preferred name
    private String ocrText;
    private String merchant;
    private BigDecimal totalAmount;
    private String currency;
    private List<ReceiptDraftItem> items;
    private List<PageOcrResult> pages;
    private List<String> warnings;
    
    // Metadata
    private String fileType;
    private Integer pdfPageCount;
    private int ocrProcessedPages;
    private String status;
    
    // New fields for validation
    private boolean needsReview;
    private BigDecimal computedTotal;
    private BigDecimal detectedTotal;
    private BigDecimal diffTotal;
    
    @Data
    @Builder
    public static class PageOcrResult {
        private int pageIndex;
        private String text;
    }
}
