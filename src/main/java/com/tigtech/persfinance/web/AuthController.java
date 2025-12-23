package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.AuthService;
import com.tigtech.persfinance.service.KeycloakAdminService;
import com.tigtech.persfinance.service.KeycloakAuthService;
import com.tigtech.persfinance.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/auth", produces = MediaType.APPLICATION_JSON_VALUE)
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

    @PostMapping(value = "/register", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Email already in use"));
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
                    .body(Map.of("error", "User created locally but failed to create in Keycloak"));
        }

        // hide password from response (using legacy setter for backward compatibility)
        user.setPassword(null);
        return ResponseEntity.ok(user);
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        if (request == null || request.getEmail() == null || request.getEmail().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "email and password are required"));
        }

        return keycloakAuthService.passwordGrant(request.getEmail(), request.getPassword());
    }

    @PostMapping(value = "/forgot", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> forgot(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok().body(Map.of("message", "If the email exists, a reset link was sent (check console in dev)"));
    }

    @PostMapping(value = "/reset", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<?> reset(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok().body(Map.of("message", "Password reset processed"));
    }

    @GetMapping("/hello")
    public Map<String, String> hello() {
        return Map.of("message", "Hello, World!");
    }

    @GetMapping("/me")
    public Map<String, Object> me(JwtAuthenticationToken auth) {
        return Map.of(
                "authenticated", true,
                "subject", auth.getName(),
                "username", auth.getToken().getClaimAsString("preferred_username"),
                "email", auth.getToken().getClaimAsString("email"),
                "roles", auth.getAuthorities().stream().map(a -> a.getAuthority()).toList()
        );
    }


    private static String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String v : values) {
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }
}
