package com.tigtech.persfinance.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Configuration
@EnableMethodSecurity(jsr250Enabled = true)
public class SecurityConfig {

    /**
     * Interface “truque” do Baeldung para ajudar com generics no DI.
     * Não tem corpo. Não tem @Bean.
     */
    interface AuthoritiesConverter extends Converter<Map<String, Object>, Collection<GrantedAuthority>> {}

    /**
     * Lê roles do Keycloak: realm_access.roles
     * e converte para authorities do Spring: ROLE_<role>
     */
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        // Allow common frontend development ports
        configuration.setAllowedOrigins(List.of("http://localhost:4200", "http://localhost:3000", "http://localhost:5173"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type")); 
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    @Bean
    AuthoritiesConverter realmRolesAuthoritiesConverter() {
        return claims -> {
            Object realmAccessObj = claims.get("realm_access");
            if (!(realmAccessObj instanceof Map<?, ?> realmAccess)) return List.of();

            Object rolesObj = realmAccess.get("roles");
            Collection<?> roles;
            if (rolesObj instanceof Collection<?> coll) {
                roles = coll;
            } else if (rolesObj instanceof Object[] arr) {
                roles = Arrays.asList(arr);
            } else {
                return List.of();
            }

            return roles.stream()
                    .filter(r -> r != null)
                    .map(Object::toString)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .map(GrantedAuthority.class::cast)
                    .toList();
        };
    }

    /**
     * Adapter do Spring: usa o AuthoritiesConverter para produzir authorities a partir do Jwt.
     */
    @Bean
    Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter(AuthoritiesConverter authoritiesConverter) {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> authoritiesConverter.convert(jwt.getClaims()));
        return converter;
    }




    @Bean
    SecurityFilterChain resourceServerSecurityFilterChain(
            HttpSecurity http,
            Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter
    ) throws Exception {

        // 1. Explicitly enable CORS using your bean
        http.cors(Customizer.withDefaults());

        http.oauth2ResourceServer(rs ->
                rs.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
        );

        http.sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        http.csrf(csrf -> csrf.disable());

        http.authorizeHttpRequests(auth -> auth
            .requestMatchers("/error").permitAll()
            .requestMatchers("/actuator/health", "/actuator/info", "/q/health/**").permitAll()
            .requestMatchers("/admin/**").hasRole("ADMIN")
            // Ensure /api/me requires a valid token (prevents NPE in controller if auth fails silently)
            .requestMatchers("/api/me").authenticated()
            .requestMatchers("/api/**").authenticated()
            .anyRequest().denyAll()
        );

        return http.build();
    }



}
