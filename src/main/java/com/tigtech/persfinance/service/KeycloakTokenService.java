package com.tigtech.persfinance.service;

import com.tigtech.persfinance.security.AuthProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
public class KeycloakTokenService {

    private static final Logger log = LoggerFactory.getLogger(KeycloakTokenService.class);

    private final RestTemplate restTemplate;
    private final AuthProvider authProvider;

    public KeycloakTokenService(RestTemplate restTemplate, AuthProvider authProvider) {
        this.restTemplate = restTemplate;
        this.authProvider = authProvider;
    }

    public String getAdminAccessToken() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id", authProvider.getKcAdminClientId());
        form.add("username", authProvider.getKcAdminUsername());
        form.add("password", authProvider.getKcAdminPassword());

        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(form, headers);

        try {
            ResponseEntity<Map> resp = restTemplate.postForEntity(authProvider.getKcAdminTokenUri(), entity, Map.class);
            if (!resp.getStatusCode().is2xxSuccessful() || resp.getBody() == null || !resp.getBody().containsKey("access_token")) {
                throw new IllegalStateException("Failed to obtain admin access token from Keycloak, status=" + resp.getStatusCode());
            }
            return (String) resp.getBody().get("access_token");
        } catch (RestClientException ex) {
            log.warn("Keycloak admin token request failed", ex);
            throw new IllegalStateException("Error obtaining admin access token from Keycloak: " + ex.getMessage(), ex);
        }
    }
}

