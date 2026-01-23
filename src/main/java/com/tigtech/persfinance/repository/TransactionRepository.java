package com.tigtech.persfinance.repository;

import com.tigtech.persfinance.domain.Transaction;
import com.tigtech.persfinance.domain.TransactionStatus;
import com.tigtech.persfinance.domain.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.createdBy.id = :userId " +
            "AND t.type = com.tigtech.persfinance.domain.TransactionType.EXPENSE " +
            "AND t.isItemized = false " +
            "AND LOWER(t.category) = LOWER(:category) " +
            "AND t.transactionDate >= :start AND t.transactionDate < :end")
    java.math.BigDecimal sumNonItemizedExpenses(@Param("userId") UUID userId,
                                                @Param("category") String category,
                                                @Param("start") Instant start,
                                                @Param("end") Instant end);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t " +
            "WHERE t.createdBy.id = :userId " +
            "AND t.type = :type " +
            "AND t.transactionDate >= :start AND t.transactionDate < :end")
    java.math.BigDecimal sumTotalByTypeAndDate(@Param("userId") UUID userId,
                                               @Param("type") com.tigtech.persfinance.domain.TransactionType type,
                                               @Param("start") Instant start,
                                               @Param("end") Instant end);

    @Query("SELECT t FROM Transaction t WHERE t.createdBy.id = :userId ORDER BY t.transactionDate DESC, t.createdAt DESC")
    List<Transaction> findRecentTransactions(@Param("userId") UUID userId, org.springframework.data.domain.Pageable pageable);

    @Query("SELECT t.category as category, SUM(t.amount) as total " +
            "FROM Transaction t " +
            "WHERE t.createdBy.id = :userId " +
            "AND t.type = com.tigtech.persfinance.domain.TransactionType.EXPENSE " +
            "AND t.isItemized = false " +
            "AND t.transactionDate >= :start AND t.transactionDate < :end " +
            "GROUP BY t.category")
    List<Object[]> sumNonItemizedExpensesByCategory(@Param("userId") UUID userId,
                                                    @Param("start") Instant start,
                                                    @Param("end") Instant end);

    @Query("SELECT t.category, SUM(t.amount) FROM Transaction t " +
            "WHERE t.createdBy.id = :userId " +
            "AND t.type = com.tigtech.persfinance.domain.TransactionType.INCOME " +
            "AND t.transactionDate >= :start AND t.transactionDate < :end " +
            "GROUP BY t.category")
    List<Object[]> sumIncomeByCategory(@Param("userId") UUID userId,
                                       @Param("start") Instant start,
                                       @Param("end") Instant end);

    // Native query or JPQL? JPQL is safer for Instant usually, but grouping by date part of instant depends on DB.
    // For MVP and H2/Postgres compatibility, we might fetch and group in memory if needed, but SQL is better.
    // Postgres: cast(transaction_date as date)
    // JPQL doesn't standarize 'date()' function well across providers without function registration.
    // Using native query for "day" truncation is safest for Postgres.
    @Query(value = "SELECT CAST(t.transaction_date AS DATE) as tdate, SUM(t.amount) " +
            "FROM transactions t " +
            "WHERE t.created_by_id = :userId " +
            "AND t.type = 'EXPENSE' " +
            "AND t.transaction_date >= :start AND t.transaction_date < :end " +
            "GROUP BY CAST(t.transaction_date AS DATE) " +
            "ORDER BY tdate ASC", nativeQuery = true)
    List<Object[]> getDailyExpenseTrendNative(@Param("userId") UUID userId,
                                        @Param("start") Instant start,
                                        @Param("end") Instant end);

    @Query("SELECT t.merchant, SUM(t.amount), COUNT(t) FROM Transaction t " +
            "WHERE t.createdBy.id = :userId " +
            "AND t.type = com.tigtech.persfinance.domain.TransactionType.EXPENSE " +
            "AND t.merchant IS NOT NULL " +
            "AND t.transactionDate >= :start AND t.transactionDate < :end " +
            "GROUP BY t.merchant " +
            "ORDER BY SUM(t.amount) DESC")
    List<Object[]> getTopMerchants(@Param("userId") UUID userId,
                                   @Param("start") Instant start,
                                   @Param("end") Instant end,
                                   org.springframework.data.domain.Pageable pageable);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.id = :transactionId
        AND (
          (t.type IN (com.tigtech.persfinance.domain.TransactionType.EXPENSE, com.tigtech.persfinance.domain.TransactionType.INCOME)
            AND t.wallet IS NOT NULL
            AND EXISTS (SELECT 1 FROM WalletMember wm WHERE wm.wallet = t.wallet AND wm.user.id = :userId))
          OR
          (t.type = com.tigtech.persfinance.domain.TransactionType.TRANSFER
            AND t.fromWallet IS NOT NULL AND t.toWallet IS NOT NULL
            AND EXISTS (SELECT 1 FROM WalletMember wm1 WHERE wm1.wallet = t.fromWallet AND wm1.user.id = :userId)
            AND EXISTS (SELECT 1 FROM WalletMember wm2 WHERE wm2.wallet = t.toWallet AND wm2.user.id = :userId))
        )
        """)
    Optional<Transaction> findByIdAndUserHasAccess(UUID transactionId, UUID userId);

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.status = :status
          AND t.transactionDate >= :fromDate
          AND t.transactionDate <= :toDate
          AND (
            :q = '' OR
            LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(t.category, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
          AND (
            (t.type IN (com.tigtech.persfinance.domain.TransactionType.EXPENSE, com.tigtech.persfinance.domain.TransactionType.INCOME)
              AND t.wallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm WHERE wm.wallet = t.wallet AND wm.user.id = :userId))
            OR
            (t.type = com.tigtech.persfinance.domain.TransactionType.TRANSFER
              AND t.fromWallet IS NOT NULL AND t.toWallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm1 WHERE wm1.wallet = t.fromWallet AND wm1.user.id = :userId)
              AND EXISTS (SELECT 1 FROM WalletMember wm2 WHERE wm2.wallet = t.toWallet AND wm2.user.id = :userId))
          )
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findAllForUserMembership(
            UUID userId,
            Instant fromDate,
            Instant toDate,
            String q,
            TransactionStatus status
    );

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.status = :status
          AND t.type = :type
          AND t.transactionDate >= :fromDate
          AND t.transactionDate <= :toDate
          AND (
            :q = '' OR
            LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(t.category, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
          AND (
            (t.type IN (com.tigtech.persfinance.domain.TransactionType.EXPENSE, com.tigtech.persfinance.domain.TransactionType.INCOME)
              AND t.wallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm WHERE wm.wallet = t.wallet AND wm.user.id = :userId))
            OR
            (t.type = com.tigtech.persfinance.domain.TransactionType.TRANSFER
              AND t.fromWallet IS NOT NULL AND t.toWallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm1 WHERE wm1.wallet = t.fromWallet AND wm1.user.id = :userId)
              AND EXISTS (SELECT 1 FROM WalletMember wm2 WHERE wm2.wallet = t.toWallet AND wm2.user.id = :userId))
          )
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findAllForUserMembershipByType(
            UUID userId,
            Instant fromDate,
            Instant toDate,
            TransactionType type,
            String q,
            TransactionStatus status
    );

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.status = :status
          AND t.transactionDate >= :fromDate
          AND t.transactionDate <= :toDate
          AND (
            (t.wallet IS NOT NULL AND t.wallet.id = :walletId) OR
            (t.fromWallet IS NOT NULL AND t.fromWallet.id = :walletId) OR
            (t.toWallet IS NOT NULL AND t.toWallet.id = :walletId)
          )
          AND (
            :q = '' OR
            LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(t.category, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
          AND (
            (t.type IN (com.tigtech.persfinance.domain.TransactionType.EXPENSE, com.tigtech.persfinance.domain.TransactionType.INCOME)
              AND t.wallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm WHERE wm.wallet = t.wallet AND wm.user.id = :userId))
            OR
            (t.type = com.tigtech.persfinance.domain.TransactionType.TRANSFER
              AND t.fromWallet IS NOT NULL AND t.toWallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm1 WHERE wm1.wallet = t.fromWallet AND wm1.user.id = :userId)
              AND EXISTS (SELECT 1 FROM WalletMember wm2 WHERE wm2.wallet = t.toWallet AND wm2.user.id = :userId))
          )
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findAllForUserMembershipByWallet(
            UUID userId,
            UUID walletId,
            Instant fromDate,
            Instant toDate,
            String q,
            TransactionStatus status
    );

    @Query("""
        SELECT t FROM Transaction t
        WHERE t.status = :status
          AND t.type = :type
          AND t.transactionDate >= :fromDate
          AND t.transactionDate <= :toDate
          AND (
            (t.wallet IS NOT NULL AND t.wallet.id = :walletId) OR
            (t.fromWallet IS NOT NULL AND t.fromWallet.id = :walletId) OR
            (t.toWallet IS NOT NULL AND t.toWallet.id = :walletId)
          )
          AND (
            :q = '' OR
            LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', :q, '%')) OR
            LOWER(COALESCE(t.category, '')) LIKE LOWER(CONCAT('%', :q, '%'))
          )
          AND (
            (t.type IN (com.tigtech.persfinance.domain.TransactionType.EXPENSE, com.tigtech.persfinance.domain.TransactionType.INCOME)
              AND t.wallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm WHERE wm.wallet = t.wallet AND wm.user.id = :userId))
            OR
            (t.type = com.tigtech.persfinance.domain.TransactionType.TRANSFER
              AND t.fromWallet IS NOT NULL AND t.toWallet IS NOT NULL
              AND EXISTS (SELECT 1 FROM WalletMember wm1 WHERE wm1.wallet = t.fromWallet AND wm1.user.id = :userId)
              AND EXISTS (SELECT 1 FROM WalletMember wm2 WHERE wm2.wallet = t.toWallet AND wm2.user.id = :userId))
          )
        ORDER BY t.transactionDate DESC
        """)
    List<Transaction> findAllForUserMembershipByWalletAndType(
            UUID userId,
            UUID walletId,
            Instant fromDate,
            Instant toDate,
            TransactionType type,
            String q,
            TransactionStatus status
    );

    @Query("""
        SELECT COUNT(t) > 0 FROM Transaction t
        WHERE t.wallet.id = :walletId
           OR t.fromWallet.id = :walletId
           OR t.toWallet.id = :walletId
    """)
    boolean hasRelatedTransactions(@Param("walletId") UUID walletId);
}
