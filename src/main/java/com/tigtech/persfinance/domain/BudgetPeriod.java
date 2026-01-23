package com.tigtech.persfinance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(
        name = "budget_periods",
        uniqueConstraints = {
                @UniqueConstraint(name = "uc_budget_period_def_dates", columnNames = {"budget_definition_id", "period_start", "period_end"})
        },
        indexes = {
                @Index(name = "idx_budget_periods_dates", columnList = "period_start, period_end")
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BudgetPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "budget_definition_id", nullable = false)
    private BudgetDefinition budgetDefinition;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "limit_amount", nullable = false, precision = 38, scale = 2)
    private BigDecimal limitAmount;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
