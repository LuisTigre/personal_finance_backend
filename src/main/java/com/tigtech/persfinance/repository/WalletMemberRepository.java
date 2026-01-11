package com.tigtech.persfinance.repository;

import com.tigtech.persfinance.domain.WalletMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface WalletMemberRepository extends JpaRepository<WalletMember, UUID> {
    
    Optional<WalletMember> findByWalletIdAndUserId(UUID walletId, UUID userId);
    
    boolean existsByWalletIdAndUserId(UUID walletId, UUID userId);

    void deleteByWalletId(UUID walletId);
}
