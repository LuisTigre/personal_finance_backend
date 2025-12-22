package com.tigtech.persfinance.security;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

// Provide test properties to avoid circular placeholder resolution for KEYCLOAK_ADMIN during context startup
@SpringBootTest(properties = {
        "keycloak.admin.username=admin",
        "keycloak.admin.password=admin",
        // provide sensible defaults so other Keycloak-related @Value placeholders resolve during tests
        "keycloak.client-id=personal-finance-api",
        "keycloak.client-secret=personal-finance-secret",
        "keycloak.token-uri=http://localhost:8888/realms/Persfin/protocol/openid-connect/token",
        "keycloak.admin-base=http://localhost:8888/admin/realms/Persfin",
        "keycloak.admin-token-uri=http://localhost:8888/realms/master/protocol/openid-connect/token"
})
@ActiveProfiles("test")
public class AuthenticationIT {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext wac;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void setup() {
        // build MockMvc with Spring Security support instead of @AutoConfigureMockMvc
        this.mockMvc = webAppContextSetup(this.wac).apply(springSecurity()).build();

        userRepository.deleteAll();
        User u = User.builder()
                .firstName("Alice")
                .lastName("Doe")
                .email("alice@example.com")
                .password("x")
                .provider("local")
                .role("ROLE_USER")
                .active(true)
                .build();
        userRepository.save(u);

        User admin = User.builder()
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .password("x")
                .provider("local")
                .role("ROLE_ADMIN")
                .active(true)
                .build();
        userRepository.save(admin);
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        Optional<User> anyUser = userRepository.findByEmail("alice@example.com");
        Long id = anyUser.map(User::getId).orElse(1L);
        mockMvc.perform(get("/api/users/" + id).accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_withValidUserToken_returns200() throws Exception {
        Optional<User> anyUser = userRepository.findByEmail("alice@example.com");
        Long id = anyUser.map(User::getId).orElse(1L);
        mockMvc.perform(get("/api/users/" + id)
                        .header("Authorization", "Bearer good-user")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @Test
    void adminOnlyEndpoint_withUserToken_returns403() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer good-user")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminOnlyEndpoint_withAdminToken_returns200() throws Exception {
        mockMvc.perform(get("/api/users")
                        .header("Authorization", "Bearer good-admin")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
    }

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        JwtDecoder jwtDecoder() {
            return token -> {
                if (token == null || token.isBlank()) throw new BadJwtException("empty");
                if ("good-user".equals(token)) {
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("sub", "user-sub");
                    claims.put("preferred_username", "alice");
                    claims.put("email", "alice@example.com");
                    Map<String, Object> realmAccess = new HashMap<>();
                    realmAccess.put("roles", new String[]{"user"});
                    claims.put("realm_access", realmAccess);
                    Instant now = Instant.now();
                    return new Jwt(token, now, now.plusSeconds(600), Map.of("alg", "RS256"), claims);
                }
                if ("good-admin".equals(token)) {
                    Map<String, Object> claims = new HashMap<>();
                    claims.put("sub", "admin-sub");
                    claims.put("preferred_username", "admin");
                    claims.put("email", "admin@example.com");
                    Map<String, Object> realmAccess = new HashMap<>();
                    realmAccess.put("roles", new String[]{"admin"});
                    claims.put("realm_access", realmAccess);
                    Instant now = Instant.now();
                    return new Jwt(token, now, now.plusSeconds(600), Map.of("alg", "RS256"), claims);
                }
                throw new BadJwtException("invalid token");
            };
        }
    }
}
