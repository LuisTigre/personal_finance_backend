package com.tigtech.persfinance.web.dto.dashboard;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardAnalyticsResponse(
    Map<String, BigDecimal> spendByCategory,
    Map<String, BigDecimal> incomeByCategory,
    Map<String, BigDecimal> spendTrend,
    Map<String, BigDecimal> topMerchants
) {}
