package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.dashboard.DashboardService;
import com.tigtech.persfinance.web.dto.dashboard.DashboardAnalyticsResponse;
import com.tigtech.persfinance.web.dto.dashboard.DashboardBudgetsResponse;
import com.tigtech.persfinance.web.dto.dashboard.DashboardOverviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@RestController
@RequestMapping("/api/dashboards")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;
    private final UserRepository userRepository;

    private User getUser(JwtAuthenticationToken principal) {
        if (principal == null || principal.getToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT");
        }
        String sub = principal.getToken().getSubject();
        return userRepository.findByKeycloakSub(sub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not registered in system"));
    }

    @GetMapping("/overview")
    public ResponseEntity<DashboardOverviewResponse> getOverview(
            JwtAuthenticationToken principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        
        LocalDate endDate = (to != null) ? to : LocalDate.now();
        LocalDate startDate = (from != null) ? from : LocalDate.now().with(TemporalAdjusters.firstDayOfMonth());

        return ResponseEntity.ok(dashboardService.getOverview(getUser(principal), startDate, endDate));
    }

    @GetMapping("/budgets")
    public ResponseEntity<DashboardBudgetsResponse> getBudgetsDashboard(JwtAuthenticationToken principal) {
        // Budget dashboard usually reflects "Current status", dates are handled by active budget periods
        return ResponseEntity.ok(dashboardService.getBudgetsDashboard(getUser(principal)));
    }

    @GetMapping("/analytics")
    public ResponseEntity<DashboardAnalyticsResponse> getAnalytics(
            JwtAuthenticationToken principal,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {

        LocalDate endDate = (to != null) ? to : LocalDate.now();
        LocalDate startDate = (from != null) ? from : LocalDate.now().minusMonths(3); // Default 3 mo history for analytics

        return ResponseEntity.ok(dashboardService.getAnalytics(getUser(principal), startDate, endDate));
    }
}
