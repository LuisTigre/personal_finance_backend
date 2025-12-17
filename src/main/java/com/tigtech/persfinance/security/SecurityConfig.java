package com.tigtech.persfinance.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.util.DefaultResourceRetriever;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        JwtAuthenticationConverter jwtAuthConverter = new JwtAuthenticationConverter();
        jwtAuthConverter.setJwtGrantedAuthoritiesConverter(this::convertJwtToAuthorities);

        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Explicitly allow the public hello endpoint and the auth endpoints
                        .requestMatchers(
                                "/api/auth/**",
                                "/actuator/**",
                                "/v3/api-docs/**",
                                "/swagger-ui/**").permitAll()
                        .anyRequest().authenticated()
                )
                .addFilterBefore(new RemoveAuthorizationHeaderFilter(), org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter.class)
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter)));

        return http.build();
    }

    // Explicit JwtDecoder bean using Nimbus RemoteJWKSet with configurable timeouts
    @Bean
    public JwtDecoder jwtDecoder(
            @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri}") String issuerUri,
            @Value("${keycloak.jwks-uri:http://127.0.0.1:8888/realms/Persfin/protocol/openid-connect/certs}") String jwkSetUri,
            @Value("${keycloak.jwks.connect-timeout-ms:2000}") int connectTimeoutMs,
            @Value("${keycloak.jwks.read-timeout-ms:5000}") int readTimeoutMs
    ) {
        try {
            // Configure Nimbus resource retriever timeouts to avoid long blocking
            DefaultResourceRetriever resourceRetriever = new DefaultResourceRetriever(connectTimeoutMs, readTimeoutMs);
            URL jwkUrl = new URL(jwkSetUri);
            // Suppress deprecation warning for RemoteJWKSet as it is still the standard way in this version of Nimbus/Spring Security
            @SuppressWarnings("deprecation")
            JWKSource<SecurityContext> jwkSource = new RemoteJWKSet<>(jwkUrl, resourceRetriever);

            ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
            JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, jwkSource);
            jwtProcessor.setJWSKeySelector(keySelector);

            NimbusJwtDecoder decoder = new NimbusJwtDecoder(jwtProcessor);
            OAuth2TokenValidator<Jwt> withIssuer = JwtValidators.createDefaultWithIssuer(issuerUri);
            decoder.setJwtValidator(withIssuer);
            return decoder;
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Invalid JWKS URL: " + jwkSetUri, e);
        }
    }

    private Collection<GrantedAuthority> convertJwtToAuthorities(Jwt jwt) {
        Collection<GrantedAuthority> authorities = new ArrayList<>();

        // add scope authorities (SCOPE_xyz)
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();
        Collection<GrantedAuthority> scopeAuth = scopeConverter.convert(jwt);
        if (scopeAuth != null) authorities.addAll(scopeAuth);

        // realm roles: jwt.claims.realm_access.roles
        Object realmAccess = jwt.getClaims().get("realm_access");
        if (realmAccess instanceof Map) {
            Object roles = ((Map<?, ?>) realmAccess).get("roles");
            if (roles instanceof Iterable) {
                for (Object r : (Iterable<?>) roles) {
                    if (r != null) authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString()));
                }
            }
        }

        // client (resource) roles: jwt.claims.resource_access.{client}.roles
        Object resourceAccess = jwt.getClaims().get("resource_access");
        if (resourceAccess instanceof Map) {
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) resourceAccess).entrySet()) {
                Object val = entry.getValue();
                if (val instanceof Map) {
                    Object roles = ((Map<?, ?>) val).get("roles");
                    if (roles instanceof Iterable) {
                        for (Object r : (Iterable<?>) roles) {
                            if (r != null) authorities.add(new SimpleGrantedAuthority("ROLE_" + r.toString()));
                        }
                    }
                }
            }
        }

        return authorities;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
