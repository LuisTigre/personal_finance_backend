package com.tigtech.persfinance.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.*;

/**
 * Extracts Keycloak roles from JWT and converts them into Spring Security authorities.
 *
 * Supported claims:
 * - realm_access.roles
 * - resource_access.<clientId>.roles (optional)
 */
public class KeycloakRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    private static final String DEFAULT_CLIENT_ID = "personal-finance-api";

    @Override
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (jwt == null) return authorities;

        Map<String, Object> claims = jwt.getClaims();
        if (claims == null) return authorities;

        addRealmRoles(claims, authorities);
        addClientRoles(claims, authorities, detectClientId(claims));

        return authorities;
    }

    private static void addRealmRoles(Map<String, Object> claims, Collection<GrantedAuthority> out) {
        Object realmAccessObj = claims.get("realm_access");
        if (!(realmAccessObj instanceof Map<?, ?> realmAccess)) return;

        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof Iterable<?> roles)) return;

        for (Object role : roles) {
            addRole(out, role);
        }
    }

    private static void addClientRoles(Map<String, Object> claims, Collection<GrantedAuthority> out, String clientId) {
        Object resourceAccessObj = claims.get("resource_access");
        if (!(resourceAccessObj instanceof Map<?, ?> resourceAccess)) return;

        Object clientAccessObj = resourceAccess.get(clientId);
        if (!(clientAccessObj instanceof Map<?, ?> clientAccess)) return;

        Object rolesObj = clientAccess.get("roles");
        if (!(rolesObj instanceof Iterable<?> roles)) return;

        for (Object role : roles) {
            addRole(out, role);
        }
    }

    private static void addRole(Collection<GrantedAuthority> out, Object roleObj) {
        if (roleObj == null) return;

        String role = roleObj.toString().trim();
        if (role.isEmpty()) return;

        out.add(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase(Locale.ROOT)));
    }

    private static String detectClientId(Map<String, Object> claims) {
        Object azp = claims.get("azp");
        if (azp instanceof String s && !s.isBlank()) {
            return s.trim();
        }
        return DEFAULT_CLIENT_ID;
    }
}

