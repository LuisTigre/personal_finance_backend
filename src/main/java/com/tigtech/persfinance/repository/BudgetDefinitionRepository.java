package com.tigtech.persfinance.repository;

import com.tigtech.persfinance.domain.BudgetDefinition;
import com.tigtech.persfinance.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface BudgetDefinitionRepository extends JpaRepository<BudgetDefinition, UUID> {
    List<BudgetDefinition> findAllByUser(User user);
    List<BudgetDefinition> findAllByUserAndIsActiveTrue(User user);
    List<BudgetDefinition> findAllByIsActiveTrue(); // For background jobs
}
