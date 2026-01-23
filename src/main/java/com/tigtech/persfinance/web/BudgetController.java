package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.budget.BudgetService;
import com.tigtech.persfinance.web.dto.budget.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/budgets")
@RequiredArgsConstructor
public class BudgetController {

    private final BudgetService budgetService;
    private final UserRepository userRepository;

    @PostMapping("/definitions")
    @ResponseStatus(HttpStatus.CREATED)
    public BudgetDefinitionResponse createDefinition(@Valid @RequestBody CreateBudgetDefinitionRequest request,
                                                     JwtAuthenticationToken principal) {
        User user = getUser(principal);
        try {
            return budgetService.createBudgetDefinition(user, request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/definitions")
    public List<BudgetDefinitionResponse> listDefinitions(JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return budgetService.listBudgetDefinitions(user);
    }

    @PutMapping("/definitions/{id}")
    public BudgetDefinitionResponse updateDefinition(@PathVariable UUID id,
                                                     @RequestBody UpdateBudgetDefinitionRequest request,
                                                     JwtAuthenticationToken principal) {
        User user = getUser(principal);
        try {
            return budgetService.updateBudgetDefinition(user, id, request);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (RuntimeException e) { // Assuming Access Denied or Not Found throws runtime logic in service
             // Map generically or specific based on message, assuming service throws better exceptions in real app
             if (e.getMessage() != null && e.getMessage().contains("Access denied")) {
                 throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
             }
             if (e.getMessage() != null && e.getMessage().contains("not found")) {
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
             }
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @DeleteMapping("/definitions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateDefinition(@PathVariable UUID id, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        try {
            budgetService.deactivateBudgetDefinition(user, id);
        } catch (RuntimeException e) {
             if (e.getMessage() != null && e.getMessage().contains("Access denied")) {
                 throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
             }
             if (e.getMessage() != null && e.getMessage().contains("not found")) {
                 throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
             }
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/summary")
    public List<BudgetPeriodSummaryResponse> getSummary(JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return budgetService.getBudgetSummary(user);
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
