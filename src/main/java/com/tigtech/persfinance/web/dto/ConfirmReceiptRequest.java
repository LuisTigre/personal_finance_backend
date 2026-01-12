package com.tigtech.persfinance.web.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmReceiptRequest {
    
    @NotNull
    private UUID walletId;
    
    @NotNull
    private Instant transactionDate;
    
    private String merchant;
    private String description;
    
    @NotNull
    @DecimalMin(value = "0.01", message = "Total amount must be positive")
    private BigDecimal totalAmount;
    
    @Builder.Default
    private String currency = "PLN";
    
    @NotEmpty(message = "At least one item is required")
    @Valid
    private List<ConfirmReceiptItem> items;
}
