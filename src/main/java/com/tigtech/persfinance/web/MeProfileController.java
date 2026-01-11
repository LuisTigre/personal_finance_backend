package com.tigtech.persfinance.web;

import com.tigtech.persfinance.service.KeycloakAdminService;
import com.tigtech.persfinance.web.dto.ProfileResponse;
import com.tigtech.persfinance.web.dto.ProfileUpdateRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/me/profile")
@RequiredArgsConstructor
public class MeProfileController {

    private final KeycloakAdminService keycloakAdminService;

    @PutMapping
    public ProfileResponse updateProfile(@AuthenticationPrincipal Jwt jwt, 
                                         @Valid @RequestBody ProfileUpdateRequest request) {
        String userId = jwt.getClaimAsString("sub");
        String email = jwt.getClaimAsString("email");
        
        keycloakAdminService.updateUserProfile(userId, request);
        
        String firstName = request.getFirstName();
        String lastName = request.getLastName();
        
        // If not provided in request, fall back to token (which might be old data, but it's the current state known to the client usually)
        if (firstName == null) {
            firstName = jwt.getClaimAsString("given_name");
        }
        if (lastName == null) {
            lastName = jwt.getClaimAsString("family_name");
        }
        
        return ProfileResponse.builder()
                .userId(userId)
                .email(email)
                .firstName(firstName)
                .lastName(lastName)
                .build();
    }
}
