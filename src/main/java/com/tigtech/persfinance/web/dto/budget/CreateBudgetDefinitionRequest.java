package com.tigtech.persfinance.web.dto.budget;

import com.tigtech.persfinance.domain.BudgetPeriodType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CreateBudgetDefinitionRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String category;

    @Size(min = 3, max = 3)
    private String currency = "PLN";

    @NotNull
    private BudgetPeriodType periodType;

    @NotNull
    @DecimalMin(value = "0.01", message = "Limit amount must be greater than 0")
    private BigDecimal limitAmount;

    private LocalDate customStartDate;
    private LocalDate customEndDate;
}
