package com.tigtech.persfinance.repository;

import com.tigtech.persfinance.domain.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, UUID> {

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(ti.amount), 0) FROM TransactionItem ti " +
            "WHERE ti.transaction.createdBy.id = :userId " +
            "AND ti.transaction.type = com.tigtech.persfinance.domain.TransactionType.EXPENSE " +
            "AND LOWER(ti.category) = LOWER(:category) " +
            "AND ti.transaction.transactionDate >= :start AND ti.transaction.transactionDate < :end")
    java.math.BigDecimal sumItemizedExpenses(@org.springframework.data.repository.query.Param("userId") java.util.UUID userId,
                                             @org.springframework.data.repository.query.Param("category") String category,
                                             @org.springframework.data.repository.query.Param("start") java.time.Instant start,
                                             @org.springframework.data.repository.query.Param("end") java.time.Instant end);

    @org.springframework.data.jpa.repository.Query("SELECT ti.category, SUM(ti.amount) " +
            "FROM TransactionItem ti " +
            "WHERE ti.transaction.createdBy.id = :userId " +
            "AND ti.transaction.type = com.tigtech.persfinance.domain.TransactionType.EXPENSE " +
            "AND ti.transaction.transactionDate >= :start AND ti.transaction.transactionDate < :end " +
            "GROUP BY ti.category")
    java.util.List<Object[]> sumItemizedExpensesByCategory(@org.springframework.data.repository.query.Param("userId") java.util.UUID userId,
                                                           @org.springframework.data.repository.query.Param("start") java.time.Instant start,
                                                           @org.springframework.data.repository.query.Param("end") java.time.Instant end);
}
