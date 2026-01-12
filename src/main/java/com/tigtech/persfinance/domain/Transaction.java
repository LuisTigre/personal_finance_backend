package com.tigtech.persfinance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "transactions",
        indexes = {
                @Index(name = "idx_transactions_transaction_date", columnList = "transaction_date"),
                @Index(name = "idx_transactions_wallet_id", columnList = "wallet_id"),
                @Index(name = "idx_transactions_from_wallet_id", columnList = "from_wallet_id"),
                @Index(name = "idx_transactions_to_wallet_id", columnList = "to_wallet_id"),
                @Index(name = "idx_transactions_created_by_id", columnList = "created_by_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private TransactionStatus status = TransactionStatus.POSTED;

    @Column(nullable = false, precision = 38, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "transaction_date", nullable = false)
    private Instant transactionDate;

    @Column
    private String category;

    @Column
    private String description;

    @Column(length = 120)
    private String merchant;

    @Column(name = "is_itemized", nullable = false)
    @Builder.Default
    private boolean isItemized = false;

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private java.util.List<TransactionItem> items = new java.util.ArrayList<>();

    // For EXPENSE / INCOME
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    // For TRANSFER
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_wallet_id")
    private Wallet fromWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_wallet_id")
    private Wallet toWallet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
        if (status == null) status = TransactionStatus.POSTED;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
