package com.tigtech.persfinance.repository;

import com.tigtech.persfinance.domain.BudgetPeriod;
import com.tigtech.persfinance.domain.BudgetDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BudgetPeriodRepository extends JpaRepository<BudgetPeriod, UUID> {

    boolean existsByBudgetDefinitionAndPeriodStartAndPeriodEnd(BudgetDefinition budgetDefinition, LocalDate periodStart, LocalDate periodEnd);

    // Find periods that overlap with the given date (usually the current date)
    // Overlap logic: start <= date AND end >= date
    @Query("SELECT bp FROM BudgetPeriod bp WHERE bp.budgetDefinition.user.id = :userId AND bp.periodStart <= :date AND bp.periodEnd >= :date")
    List<BudgetPeriod> findActivePeriodsForUser(@Param("userId") UUID userId, @Param("date") LocalDate date);

    @Query("SELECT bp FROM BudgetPeriod bp WHERE bp.budgetDefinition.user.id = :userId AND bp.periodStart >= :from AND bp.periodEnd <= :to")
    List<BudgetPeriod> findPeriodsForUserInRange(@Param("userId") UUID userId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    Optional<BudgetPeriod> findTopByBudgetDefinitionOrderByPeriodStartDesc(BudgetDefinition budgetDefinition);
}
