package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.TransactionService;
import com.tigtech.persfinance.web.dto.CreateTransactionRequest;
import com.tigtech.persfinance.web.dto.ListTransactionsResponse;
import com.tigtech.persfinance.web.dto.TransactionResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping(value = "/api/transactions", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final UserRepository userRepository;

    @PostMapping
    public TransactionResponse create(@Valid @RequestBody CreateTransactionRequest request,
                                      JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return transactionService.createTransaction(user, request);
    }

    @GetMapping
    public ListTransactionsResponse list(@RequestParam(required = false) UUID walletId,
                                         @RequestParam(required = false, name = "from") Instant from,
                                         @RequestParam(required = false, name = "to") Instant to,
                                         @RequestParam(required = false) String type,
                                         @RequestParam(required = false, name = "q") String q,
                                         @RequestParam(required = false) String status,
                                         JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return transactionService.listTransactions(user, walletId, from, to, type, q, status);
    }

    @GetMapping("/{transactionId}")
    public com.tigtech.persfinance.web.dto.TransactionDetailsResponse getDetails(@PathVariable UUID transactionId, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return transactionService.getTransactionDetails(user, transactionId);
    }

    @PutMapping("/{transactionId}/items")
    public TransactionResponse replaceItems(@PathVariable UUID transactionId,
                                            @Valid @RequestBody com.tigtech.persfinance.web.dto.ReplaceTransactionItemsRequest request,
                                            JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return transactionService.replaceTransactionItems(user, transactionId, request);
    }

    @DeleteMapping("/{transactionId}/items")
    public TransactionResponse clearItems(@PathVariable UUID transactionId, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return transactionService.clearTransactionItems(user, transactionId);
    }

    @DeleteMapping("/{transactionId}")
    public TransactionResponse delete(@PathVariable UUID transactionId, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return transactionService.softDeleteTransaction(user, transactionId);
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
