package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.WalletService;
import com.tigtech.persfinance.web.dto.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/wallets", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;
    private final UserRepository userRepository;

    @PostMapping
    public WalletResponse createWallet(@Valid @RequestBody CreateWalletRequest request, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return walletService.createWallet(user, request);
    }

    @GetMapping
    public List<WalletResponse> getWallets(JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return walletService.getWalletsForUser(user);
    }

    @GetMapping("/{walletId}")
    public WalletResponse getWallet(@PathVariable UUID walletId, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return walletService.getWalletDetails(walletId, user);
    }

    @PutMapping("/{walletId}/archive")
    public WalletResponse archiveWallet(@PathVariable UUID walletId, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return walletService.archiveWallet(walletId, user);
    }

    @PutMapping("/{walletId}/unarchive")
    public WalletResponse unarchiveWallet(@PathVariable UUID walletId, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return walletService.unarchiveWallet(walletId, user);
    }

    @DeleteMapping("/{walletId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWallet(@PathVariable UUID walletId, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        walletService.deleteWallet(walletId, user);
    }

    @PostMapping("/{walletId}/members")
    public WalletResponse addMember(@PathVariable UUID walletId, 
                                    @Valid @RequestBody AddWalletMemberRequest request, 
                                    JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return walletService.addMember(walletId, user, request);
    }

    private User getUser(JwtAuthenticationToken principal) {
        if (principal == null || principal.getToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT");
        }
        String sub = principal.getToken().getSubject();
        return userRepository.findByKeycloakSub(sub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not registered in system"));
    }
}
