package com.tigtech.persfinance.web.dto;

import com.tigtech.persfinance.domain.TransactionStatus;
import com.tigtech.persfinance.domain.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Data
@Builder
public class TransactionResponse {
    private UUID id;
    private TransactionType type;
    private TransactionStatus status;
    private BigDecimal amount;
    private String currency;
    private Instant transactionDate;
    private String category;
    private String description;
    private String merchant;
    private boolean isItemized;
    private int itemCount;
    private UUID walletId;
    private UUID fromWalletId;
    private UUID toWalletId;
    private UUID createdByUserId;
}
