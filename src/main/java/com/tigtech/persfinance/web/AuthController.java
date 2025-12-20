package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.AuthService;
import com.tigtech.persfinance.web.dto.*;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthService authService;
    private final RestTemplate restTemplate = new RestTemplate();
    private final AuthenticationManager authenticationManager;

    // Keycloak configuration (property/env driven so it works in Docker Compose too)
    @org.springframework.beans.factory.annotation.Value("${keycloak.client-id:personal-finance-api}")
    private String kcClientId;

    @org.springframework.beans.factory.annotation.Value("${keycloak.client-secret:personal-finance-secret}")
    private String kcClientSecret;

    @org.springframework.beans.factory.annotation.Value("${keycloak.token-uri:http://localhost:8888/realms/Persfin/protocol/openid-connect/token}")
    private String kcTokenUri;

    @org.springframework.beans.factory.annotation.Value("${keycloak.admin-base:http://localhost:8888/admin/realms/Persfin}")
    private String kcAdminBase;

    @org.springframework.beans.factory.annotation.Value("${keycloak.admin-token-uri:http://localhost:8888/realms/master/protocol/openid-connect/token}")
    private String kcAdminTokenUri;

    @org.springframework.beans.factory.annotation.Value("${keycloak.admin-client-id:admin-cli}")
    private String kcAdminClientId;

    @org.springframework.beans.factory.annotation.Value("${KEYCLOAK_ADMIN:admin}")
    private String kcAdminUsername;

    @org.springframework.beans.factory.annotation.Value("${KEYCLOAK_ADMIN_PASSWORD:admin}")
    private String kcAdminPassword;

    public AuthController(UserRepository userRepository,
                          PasswordEncoder passwordEncoder,
                          AuthenticationManager authenticationManager,
                          AuthService authService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.authService = authService;
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

        // Also create user in Keycloak so that Keycloak can authenticate them
        try {
            createUserInKeycloak(request);
        } catch (Exception e) {
            System.out.println("DEBUG: Failed to create user in Keycloak: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("User created locally but failed to create in Keycloak: " + e.getMessage());
        }

        // hide password from response (using legacy setter for backward compatibility)
        user.setSenha(null);
        return ResponseEntity.ok(user);
    }

    private void createUserInKeycloak(RegisterRequest request) {
        String adminAccessToken = obtainAdminAccessToken();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(adminAccessToken);

        Map<String, Object> credential = Map.of(
                "type", "password",
                "value", request.getPassword(),
                "temporary", false
        );

        Map<String, Object> kcUser = Map.of(
                "username", request.getEmail(),
                "email", request.getEmail(),
                "firstName", request.getFirstName(),
                "lastName", request.getLastName(),
                "enabled", true,
                "emailVerified", false,
                "credentials", List.of(credential)
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(kcUser, headers);
        System.out.println("DEBUG: Creating user in Keycloak for email=" + request.getEmail());

        ResponseEntity<Void> response = restTemplate.postForEntity(kcAdminBase + "/users", entity, Void.class);
        System.out.println("DEBUG: Keycloak admin create user status=" + response.getStatusCode());
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("Keycloak user creation failed with status " + response.getStatusCode());
        }
    }

    private String obtainAdminAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", kcAdminClientId);
        form.add("username", kcAdminUsername);
        form.add("password", kcAdminPassword);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(kcAdminTokenUri, entity, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || !resp.getBody().containsKey("access_token")) {
                throw new IllegalStateException("Failed to obtain admin access token from Keycloak, status=" + resp.getStatusCode());
            }
            String token = (String) resp.getBody().get("access_token");
            System.out.println("DEBUG: Obtained admin access token from Keycloak (length=" + (token != null ? token.length() : 0) + ")");
            return token;
        } catch (RestClientException ex) {
            throw new IllegalStateException("Error obtaining admin access token from Keycloak: " + ex.getMessage(), ex);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody(required = false) LoginRequest request) {
        if (request == null) {
            System.out.println("DEBUG: login request body is null or not parsed (check Content-Type header and JSON). Headers should include Content-Type: application/json");
            return ResponseEntity.badRequest().body("Request body is missing or malformed. Ensure Content-Type: application/json and a valid JSON body.");
        }
        System.out.println("DEBUG: /api/auth/login called with email='" + request.getEmail() + "'");
        if (request.getEmail() == null || request.getEmail().isBlank() ||
                request.getPassword() == null || request.getPassword().isBlank()) {
            return ResponseEntity.badRequest().body("email and password are required");
        }

        return doKeycloakPasswordGrant(request.getEmail(), request.getPassword());
    }

    private ResponseEntity<?> doKeycloakPasswordGrant(String username, String password) {
        System.out.println("DEBUG: calling Keycloak token endpoint '" + kcTokenUri + "' for user='" + username + "'");
        System.out.println("DEBUG: using client_id='" + kcClientId + "'");
        System.out.println("DEBUG: client_secret is " + (kcClientSecret != null && !kcClientSecret.isBlank() ? "PRESENT" : "MISSING"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", kcClientId);
        form.add("client_secret", kcClientSecret);
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);
        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(kcTokenUri, entity, String.class);
            System.out.println("DEBUG: Keycloak token endpoint returned status=" + resp.getStatusCode().value());
            System.out.println("DEBUG: Keycloak response headers: " + resp.getHeaders());
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (org.springframework.web.client.HttpClientErrorException | org.springframework.web.client.HttpServerErrorException e) {
            String body = e.getResponseBodyAsString();
            System.out.println("DEBUG: Keycloak returned error status=" + e.getStatusCode() + " body=" + body);
            System.out.println("DEBUG: Keycloak error headers: " + e.getResponseHeaders());
            return ResponseEntity.status(e.getStatusCode()).body(body != null ? body : e.getMessage());
        } catch (org.springframework.web.client.ResourceAccessException e) {
            System.out.println("DEBUG: ResourceAccessException when calling Keycloak: " + e.getMessage());
            return ResponseEntity.status(502).body("Unable to reach Keycloak token endpoint: " + e.getMessage());
        } catch (Exception e) {
            System.out.println("DEBUG: Exception when calling Keycloak: " + e.getMessage());
            return ResponseEntity.status(500).body("Keycloak token exchange failed: " + e.getMessage());
        }
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
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).body("Unauthenticated");
        }
        Object principal = auth.getPrincipal();
        return ResponseEntity.ok(principal);
    }

    @PostMapping("/debug-login")
    public ResponseEntity<?> debugLogin(@RequestBody(required = false) String rawBody,
                                        @RequestHeader Map<String, String> headers) {
        return ResponseEntity.ok(Map.of("body", rawBody == null ? "<null>" : rawBody, "headers", headers));
    }
}
