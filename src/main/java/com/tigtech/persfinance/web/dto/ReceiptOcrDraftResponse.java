package com.tigtech.persfinance.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReceiptOcrDraftResponse {
    private String ocrText;
    private String merchant;
    private BigDecimal totalAmount;
    private String currency;
    private List<ReceiptDraftItem> items;
    private List<String> warnings;
}
