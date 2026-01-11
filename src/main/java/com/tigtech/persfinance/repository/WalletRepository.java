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
}
