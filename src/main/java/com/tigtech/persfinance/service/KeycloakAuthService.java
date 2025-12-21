package com.tigtech.persfinance.service;

import com.tigtech.persfinance.security.AuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

@Service
public class KeycloakAuthService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAuthService.class);

    private final AuthProvider authProvider;
    private final RestTemplate restTemplate;

    public KeycloakAuthService(AuthProvider authProvider, RestTemplate restTemplate) {
        this.authProvider = authProvider;
        this.restTemplate = restTemplate;
    }

    public ResponseEntity<String> passwordGrant(String username, String password) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", authProvider.getKcClientId());
        // Only include client_secret if non-empty (public clients should omit it)
        if (authProvider.getKcClientSecret() != null && !authProvider.getKcClientSecret().isBlank()) {
            form.add("client_secret", authProvider.getKcClientSecret());
        }
        form.add("username", username);
        form.add("password", password);

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<String> resp = restTemplate.postForEntity(authProvider.getKcTokenUri(), entity, String.class);
            return ResponseEntity.status(resp.getStatusCode()).body(resp.getBody());
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            // return Keycloak's body to caller (useful for client to show invalid_grant etc)
            return ResponseEntity.status(e.getStatusCode()).body(e.getResponseBodyAsString());
        } catch (ResourceAccessException e) {
            log.warn("Unable to reach Keycloak token endpoint {}", authProvider.getKcTokenUri(), e);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body("Unable to reach Keycloak token endpoint");
        } catch (Exception e) {
            log.error("Keycloak token exchange failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Keycloak token exchange failed");
        }
    }
}
