package com.tigtech.persfinance.web.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetDetailDto(
    UUID id,
    String name,
    String category,
    String periodLabel,
    BigDecimal limit,
    BigDecimal spent,
    BigDecimal remaining,
    BigDecimal usagePct
) {}
