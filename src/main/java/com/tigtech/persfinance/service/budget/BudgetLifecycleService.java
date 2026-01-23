package com.tigtech.persfinance.service.budget;

import com.tigtech.persfinance.domain.BudgetDefinition;
import com.tigtech.persfinance.domain.BudgetPeriod;
import com.tigtech.persfinance.domain.BudgetPeriodType;
import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.BudgetDefinitionRepository;
import com.tigtech.persfinance.repository.BudgetPeriodRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BudgetLifecycleService {

    private final BudgetDefinitionRepository budgetDefinitionRepository;
    private final BudgetPeriodRepository budgetPeriodRepository;

    @Transactional
    public void ensureCurrentPeriodsExist(User user) {
        List<BudgetDefinition> definitions = budgetDefinitionRepository.findAllByUserAndIsActiveTrue(user);
        LocalDate today = LocalDate.now();

        for (BudgetDefinition def : definitions) {
            createPeriodIfNeeded(def, today);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // Daily at 2 AM
    @Transactional
    public void ensureAllActivePeriodsExist() {
        log.info("Running scheduled budget period generation...");
        List<BudgetDefinition> definitions = budgetDefinitionRepository.findAllByIsActiveTrue();
        LocalDate today = LocalDate.now();

        for (BudgetDefinition def : definitions) {
            // Optimization: could group by user, but linear is fine for MVP
            createPeriodIfNeeded(def, today);
        }
        log.info("Finished scheduled budget period generation.");
    }

    private void createPeriodIfNeeded(BudgetDefinition def, LocalDate refDate) {
        LocalDate start;
        LocalDate end;

        if (def.getPeriodType() == BudgetPeriodType.MONTHLY) {
            start = refDate.with(TemporalAdjusters.firstDayOfMonth());
            end = refDate.with(TemporalAdjusters.lastDayOfMonth());
        } else if (def.getPeriodType() == BudgetPeriodType.WEEKLY) {
            start = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            end = refDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        } else if (def.getPeriodType() == BudgetPeriodType.CUSTOM_RANGE) {
            if (def.getCustomStartDate() == null || def.getCustomEndDate() == null) {
                // Should not happen if validated, but safety check
                return;
            }
            start = def.getCustomStartDate();
            end = def.getCustomEndDate();
            
            // For custom range, we only care if the refDate is within the range or if we are just ensuring it exists?
            // "create exactly one period... idempotent"
            // We just try to create it regardless of refDate, logic works if we consider it's a "one off".
            // However, "ensureCurrentPeriodsExist" implies strictly current. 
            // But checking existence is cheap.
        } else {
            return;
        }

        if (!budgetPeriodRepository.existsByBudgetDefinitionAndPeriodStartAndPeriodEnd(def, start, end)) {
            BudgetPeriod period = BudgetPeriod.builder()
                    .budgetDefinition(def)
                    .periodStart(start)
                    .periodEnd(end)
                    .limitAmount(def.getLimitAmount())
                    .build();
            budgetPeriodRepository.save(period);
            log.info("Created budget period for definition {} [{} - {}]", def.getId(), start, end);
        }
    }
}
