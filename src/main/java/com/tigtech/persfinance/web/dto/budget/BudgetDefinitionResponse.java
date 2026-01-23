package com.tigtech.persfinance.web.dto.budget;

import com.tigtech.persfinance.domain.BudgetPeriodType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class BudgetDefinitionResponse {
    private UUID id;
    private String name;
    private String category;
    private String currency;
    private BudgetPeriodType periodType;
    private BigDecimal limitAmount;
    private LocalDate customStartDate;
    private LocalDate customEndDate;
    private boolean isActive;
}
