package com.tigtech.persfinance.web.dto.dashboard;

import java.math.BigDecimal;
import java.util.List;

public record DashboardBudgetsResponse(
    BudgetGlobalSummary summary,
    List<BudgetDetailDto> budgets
) {}
