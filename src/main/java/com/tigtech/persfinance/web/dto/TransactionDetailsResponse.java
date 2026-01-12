package com.tigtech.persfinance.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class TransactionDetailsResponse {
    private TransactionResponse transaction;
    private List<TransactionItemDto> items;
    private BigDecimal allocatedTotal;
    private boolean isBalanced;
}
