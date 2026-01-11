package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.domain.*;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.repository.WalletMemberRepository;
import com.tigtech.persfinance.repository.WalletRepository;
import com.tigtech.persfinance.service.WalletService;
import com.tigtech.persfinance.web.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class WalletServiceImpl implements WalletService {

    private final WalletRepository walletRepository;
    private final WalletMemberRepository walletMemberRepository;
    private final UserRepository userRepository;
    private final com.tigtech.persfinance.repository.TransactionRepository transactionRepository;

    @Override
    public WalletResponse createWallet(User creator, CreateWalletRequest request) {
        Wallet wallet = Wallet.builder()
                .name(request.getName())
                .currency(request.getCurrency() != null ? request.getCurrency() : "PLN")
                .initialBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .currentBalance(request.getInitialBalance() != null ? request.getInitialBalance() : BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        wallet = walletRepository.save(wallet);

        WalletMember owner = WalletMember.builder()
                .wallet(wallet)
                .user(creator)
                .role(WalletRole.OWNER)
                .build();

        walletMemberRepository.save(owner);
        wallet.getMembers().add(owner);

        return mapToResponse(wallet);
    }

    @Override
    public WalletResponse addMember(UUID walletId, User requester, AddWalletMemberRequest request) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        validateRole(wallet, requester, WalletRole.OWNER);

        User userToAdd = findUserByIdentifier(request.getIdentifier());

        if (walletMemberRepository.existsByWalletIdAndUserId(wallet.getId(), userToAdd.getId())) {
             throw new ResponseStatusException(HttpStatus.CONFLICT, "User is already a member of this wallet");
        }

        WalletMember member = WalletMember.builder()
                .wallet(wallet)
                .user(userToAdd)
                .role(request.getRole())
                .build();

        walletMemberRepository.save(member);
        
        // Refresh wallet to include new member in response
        return getWalletDetails(walletId, requester);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsForUser(User user) {
        return walletRepository.findAllByUserId(user.getId()).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public WalletResponse getWalletDetails(UUID walletId, User user) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        if (!walletMemberRepository.existsByWalletIdAndUserId(wallet.getId(), user.getId())) {
             throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied");
        }

        return mapToResponse(wallet);
    }

    @Override
    public WalletResponse archiveWallet(UUID walletId, User user) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        validateRole(wallet, user, WalletRole.OWNER);

        wallet.setStatus(WalletStatus.ARCHIVED);
        wallet = walletRepository.save(wallet);

        return mapToResponse(wallet);
    }

    @Override
    public WalletResponse unarchiveWallet(UUID walletId, User user) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        validateRole(wallet, user, WalletRole.OWNER);

        wallet.setStatus(WalletStatus.ACTIVE);
        wallet = walletRepository.save(wallet);

        return mapToResponse(wallet);
    }

    @Override
    public void deleteWallet(UUID walletId, User user) {
        Wallet wallet = walletRepository.findById(walletId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        validateRole(wallet, user, WalletRole.OWNER);

        if (transactionRepository.hasRelatedTransactions(walletId)) {
            // Soft delete (archive) if transactions exist
            wallet.setStatus(WalletStatus.ARCHIVED);
            walletRepository.save(wallet);
        } else {
            // Hard delete if no transactions
            walletMemberRepository.deleteByWalletId(walletId); // Explicitly delete members first to avoid constraint issues if cascade is missing
            walletRepository.delete(wallet);
        }
    }

    private void validateRole(Wallet wallet, User user, WalletRole requiredRole) {
        WalletMember member = walletMemberRepository.findByWalletIdAndUserId(wallet.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        if (member.getRole() != requiredRole) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private User findUserByIdentifier(String identifier) {
        // Try email first
        Optional<User> byEmail = userRepository.findByEmail(identifier);
        if (byEmail.isPresent()) return byEmail.get();

        // Try UUID
        try {
            UUID id = UUID.fromString(identifier);
            Optional<User> byId = userRepository.findById(id);
            if (byId.isPresent()) return byId.get();
        } catch (IllegalArgumentException ignored) {
            // Not a UUID
        }
        
        // Given existing User entity doesn't have a unique 'username' field (only displayName),
        // and we prioritized email/UUID. If 'username' was intended to be keycloakSub or similar unique field,
        // we could check that. For now throw not found for "username" if it doesn't match email.
        
        throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found with identifier: " + identifier);
    }

    private WalletResponse mapToResponse(Wallet wallet) {
        return WalletResponse.builder()
                .id(wallet.getId())
                .name(wallet.getName())
                .currency(wallet.getCurrency())
                .currentBalance(wallet.getCurrentBalance())
                .status(wallet.getStatus())
                .createdAt(wallet.getCreatedAt())
                .members(wallet.getMembers().stream()
                        .map(m -> WalletMemberResponse.builder()
                                .userId(m.getUser().getId())
                                .email(m.getUser().getEmail())
                                .displayName(m.getUser().getDisplayName())
                                .role(m.getRole())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
