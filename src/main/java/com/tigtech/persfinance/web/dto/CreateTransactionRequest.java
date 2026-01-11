package com.tigtech.persfinance.web.dto;

import com.tigtech.persfinance.domain.TransactionType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
public class CreateTransactionRequest {

    @NotNull
    private TransactionType type;

    @NotNull
    @Positive
    private BigDecimal amount;

    @NotNull
    private Instant transactionDate;

    private String category;

    private String description;

    // For EXPENSE / INCOME
    private UUID walletId;

    // For TRANSFER
    private UUID fromWalletId;
    private UUID toWalletId;
}
