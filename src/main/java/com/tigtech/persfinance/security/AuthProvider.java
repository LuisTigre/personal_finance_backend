package com.tigtech.persfinance.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthProvider {

    // Keycloak configuration (property/env driven so it works in Docker Compose too)
    @Value("${keycloak.client-id:personal-finance-api}")
    private String kcClientId;

    @Value("${keycloak.client-secret:personal-finance-secret}")
    private String kcClientSecret;

    @Value("${keycloak.token-uri:http://localhost:8888/realms/Persfin/protocol/openid-connect/token}")
    private String kcTokenUri;

    @Value("${keycloak.admin-base:http://localhost:8888/admin/realms/Persfin}")
    private String kcAdminBase;

    @Value("${keycloak.admin-token-uri:http://localhost:8888/realms/master/protocol/openid-connect/token}")
    private String kcAdminTokenUri;

    @Value("${keycloak.admin-client-id:admin-cli}")
    private String kcAdminClientId;

    @Value("${KEYCLOAK_ADMIN:admin}")
    private String kcAdminUsername;

    @Value("${KEYCLOAK_ADMIN_PASSWORD:admin}")
    private String kcAdminPassword;

    public String getKcClientId() {
        return kcClientId;
    }

    public String getKcClientSecret() {
        return kcClientSecret;
    }

    public String getKcTokenUri() {
        return kcTokenUri;
    }

    public String getKcAdminBase() {
        return kcAdminBase;
    }

    public String getKcAdminTokenUri() {
        return kcAdminTokenUri;
    }

    public String getKcAdminClientId() {
        return kcAdminClientId;
    }

    public String getKcAdminUsername() {
        return kcAdminUsername;
    }

    public String getKcAdminPassword() {
        return kcAdminPassword;
    }
}

