package com.tigtech.persfinance.service.dashboard;

import com.tigtech.persfinance.domain.Transaction;
import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.domain.TransactionType;
import com.tigtech.persfinance.repository.TransactionItemRepository;
import com.tigtech.persfinance.repository.TransactionRepository;
import com.tigtech.persfinance.repository.WalletRepository;
import com.tigtech.persfinance.service.budget.BudgetService;
import com.tigtech.persfinance.web.dto.budget.BudgetPeriodSummaryResponse;
import com.tigtech.persfinance.web.dto.dashboard.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;
    private final WalletRepository walletRepository;
    private final BudgetService budgetService; 

    @Transactional(readOnly = true)
    public DashboardOverviewResponse getOverview(User user, LocalDate from, LocalDate to) {
        // 1. Balances
        BigDecimal totalBalance = walletRepository.sumTotalBalanceByUser(user.getId());

        // Convert LocalDate to Instant for DB queries (assuming UTC for simplicity as per MVP)
        // In a real app, we'd use the user's timezone.
        java.time.Instant start = from.atStartOfDay(ZoneId.of("UTC")).toInstant();
        java.time.Instant end = to.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        // 2. Cash Flow
        BigDecimal income = transactionRepository.sumTotalByTypeAndDate(user.getId(), TransactionType.INCOME, start, end);
        BigDecimal expense = transactionRepository.sumTotalByTypeAndDate(user.getId(), TransactionType.EXPENSE, start, end);
        BigDecimal net = income.subtract(expense);

        // 3. Recent Transactions
        List<TransactionPreviewDto> recent = transactionRepository.findRecentTransactions(user.getId(), PageRequest.of(0, 5))
                .stream()
                .map(this::mapToPreview)
                .toList();

        // 4. Budget Alerts (Top 5 by usage %)
        // Reuse strict calculation logic from BudgetService
        List<BudgetPeriodSummaryResponse> allBudgets = budgetService.getBudgetSummary(user);
        List<BudgetAlertDto> alerts = allBudgets.stream()
                .sorted(Comparator.comparing(BudgetPeriodSummaryResponse::getProgressPercent).reversed())
                .limit(5)
                .map(b -> new BudgetAlertDto(
                        b.getBudgetDefinitionId(),
                        b.getName(),
                        b.getLimitAmount(),
                        b.getSpentAmount(),
                        b.getRemainingAmount(),
                        b.getProgressPercent()
                ))
                .toList();

        return new DashboardOverviewResponse(totalBalance, income, expense, net, recent, alerts);
    }

    @Transactional(readOnly = true)
    public DashboardBudgetsResponse getBudgetsDashboard(User user) {
        // We reuse the BudgetService provided earlier to get accurate calculations
        List<BudgetPeriodSummaryResponse> periods = budgetService.getBudgetSummary(user);

        BigDecimal totalLimit = BigDecimal.ZERO;
        BigDecimal totalSpent = BigDecimal.ZERO;

        List<BudgetDetailDto> details = new ArrayList<>();

        for (BudgetPeriodSummaryResponse p : periods) {
            totalLimit = totalLimit.add(p.getLimitAmount());
            totalSpent = totalSpent.add(p.getSpentAmount());
            
            details.add(new BudgetDetailDto(
                    p.getBudgetPeriodId(),
                    p.getName(),
                    p.getCategory(),
                    p.getPeriodStart() + " - " + p.getPeriodEnd(),
                    p.getLimitAmount(),
                    p.getSpentAmount(),
                    p.getRemainingAmount(),
                    p.getProgressPercent()
            ));
        }

        BudgetGlobalSummary summary = new BudgetGlobalSummary(
                totalLimit, 
                totalSpent, 
                totalLimit.subtract(totalSpent)
        );

        return new DashboardBudgetsResponse(summary, details);
    }

    @Transactional(readOnly = true)
    public DashboardAnalyticsResponse getAnalytics(User user, LocalDate from, LocalDate to) {
        java.time.Instant start = from.atStartOfDay(ZoneId.of("UTC")).toInstant();
        java.time.Instant end = to.plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();

        // 1. Spend by Category (Merge Itemized + Non-Itemized)
        Map<String, BigDecimal> spendMap = new HashMap<>();
        
        // A. Non-Itemized
        List<Object[]> nonItemized = transactionRepository.sumNonItemizedExpensesByCategory(user.getId(), start, end);
        mergeIntoMap(spendMap, nonItemized);
        
        // B. Itemized
        List<Object[]> itemized = transactionItemRepository.sumItemizedExpensesByCategory(user.getId(), start, end);
        mergeIntoMap(spendMap, itemized);

        // 2. Income by Category
        Map<String, BigDecimal> incomeMap = new HashMap<>();
        List<Object[]> incomeData = transactionRepository.sumIncomeByCategory(user.getId(), start, end);
        mergeIntoMap(incomeMap, incomeData);

        // 3. Trend (Group by Date) - Currently supporting Daily only for MVP
        // Creating a full TreeMap to ensure sorting
        Map<String, BigDecimal> trendMap = new TreeMap<>(); 
        List<Object[]> dailyData = transactionRepository.getDailyExpenseTrendNative(user.getId(), start, end);
        
        for (Object[] row : dailyData) {
            // Native query returns java.sql.Date or java.util.Date depending on driver
            // We cast to string for the map key
            Object dateObj = row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (dateObj != null && amount != null) {
                trendMap.put(dateObj.toString(), amount);
            }
        }

        // 4. Top Merchants
        Map<String, BigDecimal> topMerchants = new LinkedHashMap<>();
        List<Object[]> merchantsData = transactionRepository.getTopMerchants(user.getId(), start, end, PageRequest.of(0, 10));
        for (Object[] row : merchantsData) {
            String merchant = (String) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            // Long count = (Long) row[2]; // Unused in DTO map for now
            if (merchant != null && amount != null) {
                topMerchants.put(merchant, amount);
            }
        }

        return new DashboardAnalyticsResponse(spendMap, incomeMap, trendMap, topMerchants);
    }

    // Helper to merge Object[] results (category, amount) into map
    private void mergeIntoMap(Map<String, BigDecimal> map, List<Object[]> data) {
        for (Object[] row : data) {
            String cat = (String) row[0];
            BigDecimal amt = (BigDecimal) row[1];
            if (cat != null && amt != null) {
                map.merge(cat, amt, BigDecimal::add);
            }
        }
    }

    private TransactionPreviewDto mapToPreview(Transaction t) {
        return new TransactionPreviewDto(
                t.getId(),
                t.getAmount(),
                t.getCategory(),
                t.getCurrency(),
                t.getTransactionDate(), // Instant
                t.getType().name(),
                t.getDescription()
        );
    }
}
