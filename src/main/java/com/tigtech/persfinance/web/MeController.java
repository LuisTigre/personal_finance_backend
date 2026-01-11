package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.domain.UserStatus;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.web.dto.UserResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/api/me", produces = MediaType.APPLICATION_JSON_VALUE)
public class MeController {

    private final UserRepository userRepository;

    public MeController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public UserResponse me(JwtAuthenticationToken authentication) {
        String sub = authentication.getToken().getSubject();
        String username = authentication.getToken().getClaimAsString("preferred_username");
        String email = authentication.getToken().getClaimAsString("email");
        String name = authentication.getToken().getClaimAsString("name");
        String givenName = authentication.getToken().getClaimAsString("given_name");
        String familyName = authentication.getToken().getClaimAsString("family_name");
        
        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();
        
        // JIT Provisioning / Update
        User user = userRepository.findByKeycloakSub(sub)
            .map(existing -> {
                // Update cache fields & last seen
                existing.setLastSeenAt(LocalDateTime.now());
                if (email != null) existing.setEmail(email);
                if (name != null) existing.setDisplayName(name);
                return userRepository.save(existing);
            })
            .orElseGet(() -> {
                // Create new user
                User newUser = User.builder()
                        .keycloakSub(sub)
                        .email(email)
                        .displayName(name != null ? name : username)
                        .status(UserStatus.ACTIVE)
                        .lastSeenAt(LocalDateTime.now())
                        .build();
                return userRepository.save(newUser);
            });

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFirstName(givenName != null ? givenName : user.getDisplayName());
        response.setLastName(familyName != null ? familyName : "");
        response.setEmail(user.getEmail());
        response.setPhotoUrl(user.getProfilePictureUrl());
        response.setActive(user.getStatus() == UserStatus.ACTIVE);
        
        // Map roles simply (e.g. pick first or join)
        // Contract says "role": "ROLE_USER" (single string)
        // We can pick the most significant one or just the first
        response.setRole(roles.isEmpty() ? "ROLE_USER" : roles.get(0));

        return response;
    }
}

