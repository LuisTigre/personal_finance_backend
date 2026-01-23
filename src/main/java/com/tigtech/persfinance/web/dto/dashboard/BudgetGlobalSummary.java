package com.tigtech.persfinance.web.dto.dashboard;

import java.math.BigDecimal;

public record BudgetGlobalSummary(
    BigDecimal totalLimit,
    BigDecimal totalSpent,
    BigDecimal totalRemaining
) {}
