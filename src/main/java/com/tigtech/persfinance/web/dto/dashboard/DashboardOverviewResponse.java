package com.tigtech.persfinance.web.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DashboardOverviewResponse(
    BigDecimal totalBalance,
    BigDecimal incomeTotal,
    BigDecimal expenseTotal,
    BigDecimal netCashFlow,
    List<TransactionPreviewDto> recentTransactions,
    List<BudgetAlertDto> budgetAlerts
) {}
