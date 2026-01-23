package com.tigtech.persfinance.service.budget;

import com.tigtech.persfinance.domain.*;
import com.tigtech.persfinance.repository.*;
import com.tigtech.persfinance.web.dto.budget.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetDefinitionRepository budgetDefinitionRepository;
    private final BudgetPeriodRepository budgetPeriodRepository;
    private final BudgetLifecycleService budgetLifecycleService;
    private final TransactionRepository transactionRepository;
    private final TransactionItemRepository transactionItemRepository;

    @Transactional
    public BudgetDefinitionResponse createBudgetDefinition(User user, CreateBudgetDefinitionRequest request) {
        if (request.getPeriodType() == BudgetPeriodType.CUSTOM_RANGE) {
             if (request.getCustomStartDate() == null || request.getCustomEndDate() == null) {
                 throw new IllegalArgumentException("Custom start/end dates required for CUSTOM_RANGE");
             }
             if (request.getCustomStartDate().isAfter(request.getCustomEndDate())) {
                 throw new IllegalArgumentException("Start date must be before end date");
             }
        }

        BudgetDefinition def = BudgetDefinition.builder()
                .user(user)
                .name(request.getName())
                .category(request.getCategory())
                .currency(request.getCurrency())
                .periodType(request.getPeriodType())
                .limitAmount(request.getLimitAmount())
                .customStartDate(request.getCustomStartDate())
                .customEndDate(request.getCustomEndDate())
                .isActive(true)
                .build();
        
        def = budgetDefinitionRepository.save(def);
        
        // Immediately trigger period creation
        budgetLifecycleService.ensureCurrentPeriodsExist(user);

        return mapToResponse(def);
    }

    @Transactional(readOnly = true)
    public List<BudgetDefinitionResponse> listBudgetDefinitions(User user) {
        return budgetDefinitionRepository.findAllByUser(user).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BudgetDefinitionResponse updateBudgetDefinition(User user, UUID id, UpdateBudgetDefinitionRequest request) {
        BudgetDefinition def = budgetDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget definition not found"));

        if (!def.getUser().getId().equals(user.getId())) {
             throw new RuntimeException("Access denied"); 
        }

        if (request.getName() != null) def.setName(request.getName());
        if (request.getCategory() != null) def.setCategory(request.getCategory());
        if (request.getCurrency() != null) def.setCurrency(request.getCurrency());
        if (request.getLimitAmount() != null) def.setLimitAmount(request.getLimitAmount());
        if (request.getPeriodType() != null) def.setPeriodType(request.getPeriodType());
        if (request.getCustomStartDate() != null) def.setCustomStartDate(request.getCustomStartDate());
        if (request.getCustomEndDate() != null) def.setCustomEndDate(request.getCustomEndDate());
        if (request.getIsActive() != null) def.setIsActive(request.getIsActive());

        // Validate types again if changed
        if (def.getPeriodType() == BudgetPeriodType.CUSTOM_RANGE) {
               if (def.getCustomStartDate() == null || def.getCustomEndDate() == null) {
                   throw new IllegalArgumentException("Custom start/end dates required for CUSTOM_RANGE");
               }
               if (def.getCustomStartDate().isAfter(def.getCustomEndDate())) {
                   throw new IllegalArgumentException("Start date must be before end date");
               }
        }

        def = budgetDefinitionRepository.save(def);
        return mapToResponse(def);
    }
    
    @Transactional
    public void deactivateBudgetDefinition(User user, UUID id) {
        BudgetDefinition def = budgetDefinitionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Budget definition not found"));
        if (!def.getUser().getId().equals(user.getId())) {
            throw new RuntimeException("Access denied");
        }
        def.setIsActive(false);
        budgetDefinitionRepository.save(def);
    }

    @Transactional
    public List<BudgetPeriodSummaryResponse> getBudgetSummary(User user) {
        budgetLifecycleService.ensureCurrentPeriodsExist(user);
        LocalDate today = LocalDate.now();
        List<BudgetPeriod> periods = budgetPeriodRepository.findActivePeriodsForUser(user.getId(), today);
        return periods.stream().map(p -> mapToSummary(p, user.getId())).collect(Collectors.toList());
    }

    private BudgetDefinitionResponse mapToResponse(BudgetDefinition def) {
        return BudgetDefinitionResponse.builder()
                .id(def.getId())
                .name(def.getName())
                .category(def.getCategory())
                .currency(def.getCurrency())
                .periodType(def.getPeriodType())
                .limitAmount(def.getLimitAmount())
                .customStartDate(def.getCustomStartDate())
                .customEndDate(def.getCustomEndDate())
                .isActive(def.isActive())
                .build();
    }

    private BudgetPeriodSummaryResponse mapToSummary(BudgetPeriod period, UUID userId) {
        BigDecimal spent = calculateSpent(period, userId);
        BigDecimal remaining = period.getLimitAmount().subtract(spent);
        BigDecimal progress;
        if (period.getLimitAmount().compareTo(BigDecimal.ZERO) == 0) {
            progress = spent.compareTo(BigDecimal.ZERO) > 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
        } else {
             progress = spent.multiply(BigDecimal.valueOf(100)).divide(period.getLimitAmount(), 2, java.math.RoundingMode.HALF_UP);
        }

        return BudgetPeriodSummaryResponse.builder()
                .budgetDefinitionId(period.getBudgetDefinition().getId())
                .budgetPeriodId(period.getId())
                .name(period.getBudgetDefinition().getName())
                .category(period.getBudgetDefinition().getCategory())
                .periodType(period.getBudgetDefinition().getPeriodType())
                .periodStart(period.getPeriodStart())
                .periodEnd(period.getPeriodEnd())
                .currency(period.getBudgetDefinition().getCurrency())
                .limitAmount(period.getLimitAmount())
                .spentAmount(spent)
                .remainingAmount(remaining)
                .progressPercent(progress)
                .isOverspent(remaining.compareTo(BigDecimal.ZERO) < 0)
                .build();
    }

    private BigDecimal calculateSpent(BudgetPeriod period, UUID userId) {
        // Timezone: UTC
        Instant start = period.getPeriodStart().atStartOfDay(ZoneId.of("UTC")).toInstant();
        Instant end = period.getPeriodEnd().plusDays(1).atStartOfDay(ZoneId.of("UTC")).toInstant();
        String category = period.getBudgetDefinition().getCategory();

        BigDecimal nonItemized = transactionRepository.sumNonItemizedExpenses(userId, category, start, end);
        BigDecimal itemized = transactionItemRepository.sumItemizedExpenses(userId, category, start, end);
        
        if (nonItemized == null) nonItemized = BigDecimal.ZERO;
        if (itemized == null) itemized = BigDecimal.ZERO;

        return nonItemized.add(itemized);
    }
}
