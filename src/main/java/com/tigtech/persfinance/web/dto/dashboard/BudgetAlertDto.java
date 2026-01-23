package com.tigtech.persfinance.web.dto.dashboard;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetAlertDto(
    UUID budgetId,
    String name,
    BigDecimal limit,
    BigDecimal spent,
    BigDecimal remaining,
    BigDecimal usagePct
) {}
