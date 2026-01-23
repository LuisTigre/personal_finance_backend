package com.tigtech.persfinance.web.dto.budget;

import com.tigtech.persfinance.domain.BudgetPeriodType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BudgetPeriodSummaryResponse {
    private UUID budgetDefinitionId;
    private UUID budgetPeriodId;
    private String name;
    private String category;
    private BudgetPeriodType periodType;
    private LocalDate periodStart;
    private LocalDate periodEnd;
    private String currency;
    private BigDecimal limitAmount;
    private BigDecimal spentAmount;
    private BigDecimal remainingAmount;
    private BigDecimal progressPercent;
    private boolean isOverspent;
}
