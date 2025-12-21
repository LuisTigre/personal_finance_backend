package com.tigtech.persfinance.service;

import com.tigtech.persfinance.security.AuthProvider;
import com.tigtech.persfinance.web.dto.RegisterRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
public class KeycloakAdminService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminService.class);

    private final RestTemplate restTemplate;
    private final AuthProvider authProvider;
    private final KeycloakTokenService keycloakTokenService;

    public KeycloakAdminService(RestTemplate restTemplate, AuthProvider authProvider, KeycloakTokenService keycloakTokenService) {
        this.restTemplate = restTemplate;
        this.authProvider = authProvider;
        this.keycloakTokenService = keycloakTokenService;
    }

    public void createUser(RegisterRequest request) {
        String adminAccessToken = keycloakTokenService.getAdminAccessToken();

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

        try {
            ResponseEntity<Void> response = restTemplate.postForEntity(authProvider.getKcAdminBase() + "/users", entity, Void.class);
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("Keycloak user creation failed with status " + response.getStatusCode());
            }
        } catch (RestClientException ex) {
            log.warn("Keycloak admin create user failed", ex);
            throw new IllegalStateException("Keycloak admin create user failed: " + ex.getMessage(), ex);
        }
    }
}
