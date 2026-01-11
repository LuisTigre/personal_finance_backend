package com.tigtech.persfinance.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class CreateWalletRequest {
    @NotBlank
    private String name;
    
    private String currency = "PLN";
    
    private BigDecimal initialBalance;
}
