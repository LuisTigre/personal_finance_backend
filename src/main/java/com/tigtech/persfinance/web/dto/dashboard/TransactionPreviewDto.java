package com.tigtech.persfinance.web.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;
import java.time.Instant;

public record TransactionPreviewDto(
    UUID id,
    BigDecimal amount,
    String category,
    String currency,
    Instant date,
    String type,
    String description
) {}
