package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.AuthService;
import com.tigtech.persfinance.service.KeycloakAdminService;
import com.tigtech.persfinance.service.KeycloakAuthService;
import com.tigtech.persfinance.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final KeycloakAdminService keycloakAdminService;
    private final KeycloakAuthService keycloakAuthService;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthService authService,
                          KeycloakAdminService keycloakAdminService,
                          KeycloakAuthService keycloakAuthService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authService = authService;
        this.keycloakAdminService = keycloakAdminService;
        this.keycloakAuthService = keycloakAuthService;
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body("Email already in use");
        }

        User user = User.builder()
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .provider("local")
                .role("ROLE_USER")
                .active(true)
                .build();
        userRepository.save(user);

        try {
            keycloakAdminService.createUser(request);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("User created locally but failed to create in Keycloak");
        }

        // hide password from response (using legacy setter for backward compatibility)
        user.setSenha(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("email and password are required");
        }

        return keycloakAuthService.passwordGrant(request.getEmail(), request.getPassword());
    }

    @PostMapping("/forgot")
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().body("If the email exists, a reset link was sent (check console in dev)");
    }

    @PostMapping("/reset")
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().body("Password reset processed");
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello, World!";
    }

    @GetMapping("/me")
    public ResponseEntity<?> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return ResponseEntity.status(401).body(Map.of("authenticated", false));
        }

        MeResponse resp = new MeResponse();
        resp.setAuthenticated(true);
        resp.setName(auth.getName());
        resp.setRoles(auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> a != null && !a.isBlank())
                .sorted()
                .collect(Collectors.toList()));

        Object principal = auth.getPrincipal();
        if (principal instanceof Jwt jwt) {
            resp.setSubject(jwt.getSubject());
            resp.setIssuer(jwt.getIssuer() != null ? jwt.getIssuer().toString() : null);
            resp.setClientId(jwt.getClaimAsString("azp"));
            resp.setUsername(firstNonBlank(
                    jwt.getClaimAsString("preferred_username"),
                    jwt.getClaimAsString("username"),
                    jwt.getClaimAsString("email"),
                    auth.getName()
            ));
            resp.setEmail(jwt.getClaimAsString("email"));

            Instant iat = jwt.getIssuedAt();
            Instant exp = jwt.getExpiresAt();
            resp.setIssuedAt(iat != null ? iat.getEpochSecond() : null);
            resp.setExpiresAt(exp != null ? exp.getEpochSecond() : null);

            String scope = jwt.getClaimAsString("scope");
            if (scope != null && !scope.isBlank()) {
                resp.setScopes(List.of(scope.split("\\s+")));
            } else {
                resp.setScopes(Collections.emptyList());
            }

            // Do NOT return full claims by default (keep response minimal)
            resp.setClaims(null);
        }

        return ResponseEntity.ok(resp);
    }

    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
