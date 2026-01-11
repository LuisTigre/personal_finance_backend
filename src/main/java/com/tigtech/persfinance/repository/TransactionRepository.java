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
