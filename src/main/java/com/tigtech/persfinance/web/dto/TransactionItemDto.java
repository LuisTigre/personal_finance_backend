package com.tigtech.persfinance.web.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionItemDto {
    private UUID id;
    private String name;
    private String category;
    private BigDecimal amount;
    private String note;
}
