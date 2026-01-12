package com.tigtech.persfinance.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ReceiptDraftItem {
    private String name;
    private BigDecimal amount;
    private String category;
}
