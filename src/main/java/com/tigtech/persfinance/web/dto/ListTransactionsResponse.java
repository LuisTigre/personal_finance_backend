package com.tigtech.persfinance.web.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ListTransactionsResponse {

    private List<TransactionResponse> items;

    private Totals totals;

    @Data
    @Builder
    public static class Totals {
        private BigDecimal totalIncome;
        private BigDecimal totalExpense;
        private BigDecimal net;
    }
}
