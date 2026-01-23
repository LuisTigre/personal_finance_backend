package com.tigtech.persfinance.repository;

import com.tigtech.persfinance.domain.Wallet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WalletRepository extends JpaRepository<Wallet, UUID> {
    
    @Query("SELECT DISTINCT w FROM Wallet w JOIN w.members m WHERE m.user.id = :userId")
    List<Wallet> findAllByUserId(UUID userId);

    @Query("SELECT COALESCE(SUM(w.currentBalance), 0) FROM Wallet w JOIN w.members m WHERE m.user.id = :userId AND w.status = com.tigtech.persfinance.domain.WalletStatus.ACTIVE")
    java.math.BigDecimal sumTotalBalanceByUser(@org.springframework.data.repository.query.Param("userId") UUID userId);
}
