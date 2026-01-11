package com.tigtech.persfinance.service;

import com.tigtech.persfinance.web.dto.ProfileResponse;
import com.tigtech.persfinance.web.dto.ProfileUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class KeycloakAdminService {

    private final RestTemplate restTemplate;

    @Value("${keycloak.base-url}")
    private String baseUrl;

    @Value("${keycloak.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    private String cachedAccessToken;
    private Instant tokenExpiry = Instant.MIN;

    public void updateUserProfile(String userId, ProfileUpdateRequest request) {
        String token = getAdminToken();

        String url = String.format("%s/admin/realms/%s/users/%s", baseUrl, realm, userId);

        Map<String, Object> body = new HashMap<>();
        if (request.getFirstName() != null) {
            body.put("firstName", request.getFirstName());
        }
        if (request.getLastName() != null) {
            body.put("lastName", request.getLastName());
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            restTemplate.exchange(url, HttpMethod.PUT, entity, Void.class);
            log.info("Successfully updated Keycloak profile for user {}", userId);
        } catch (HttpClientErrorException e) {
            log.error("Failed to update user profile in Keycloak: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found in identity provider");
            } else if (e.getStatusCode() == HttpStatus.UNAUTHORIZED || e.getStatusCode() == HttpStatus.FORBIDDEN) {
                // Token might be expired or permissions wrong. 
                // Simple retry logic could be added here if token expired, but keeping it simple for now.
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Identity provider permission error");
            }
            throw new ResponseStatusException(e.getStatusCode(), "Identity provider error");
        } catch (Exception e) {
            log.error("Unexpected error updating user profile", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update profile");
        }
    }

    private synchronized String getAdminToken() {
        if (cachedAccessToken != null && Instant.now().isBefore(tokenExpiry.minusSeconds(10))) {
            return cachedAccessToken;
        }

        log.debug("Fetching new Keycloak admin access token");
        String url = String.format("%s/realms/%s/protocol/openid-connect/token", baseUrl, realm);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);
            
            if (response != null && response.containsKey("access_token")) {
                cachedAccessToken = (String) response.get("access_token");
                Integer expiresIn = (Integer) response.get("expires_in");
                tokenExpiry = Instant.now().plusSeconds(expiresIn != null ? expiresIn : 300);
                return cachedAccessToken;
            } else {
                throw new IllegalStateException("Invalid response from Keycloak token endpoint");
            }
        } catch (Exception e) {
            log.error("Failed to obtain admin access token", e);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Could not authenticate with identity provider");
        }
    }
}
