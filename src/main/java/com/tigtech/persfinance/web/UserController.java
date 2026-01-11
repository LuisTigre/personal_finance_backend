package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.KeycloakAdminService;
import com.tigtech.persfinance.storage.StorageService;
import com.tigtech.persfinance.web.dto.ProfileUpdateRequest;
import com.tigtech.persfinance.web.dto.UserResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UserRepository userRepository;
    private final StorageService storageService;
    private final KeycloakAdminService keycloakAdminService;

    public UserController(UserRepository userRepository, StorageService storageService, KeycloakAdminService keycloakAdminService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
        this.keycloakAdminService = keycloakAdminService;
    }

    @GetMapping
    @RolesAllowed("ADMIN")
    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @RolesAllowed("ADMIN")
    public ResponseEntity<UserResponse> get(@PathVariable UUID id) {
        return userRepository.findById(id).map(u -> ResponseEntity.ok(toDto(u))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/photo")
    @RolesAllowed("ADMIN")
    public ResponseEntity<?> uploadPhoto(@PathVariable UUID id, @RequestParam("file") MultipartFile file) {
        return userRepository.findById(id).map(user -> {
            try {
                String url = storageService.uploadUserPhoto(file, String.valueOf(user.getId()));
                user.setProfilePictureUrl(url);
                userRepository.save(user);
                return ResponseEntity.ok().body(new UserResponse());
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Void> updateUser(@PathVariable UUID id, @RequestBody @Valid ProfileUpdateRequest request, Authentication authentication) {
        return userRepository.findById(id).map(user -> {
            boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            
            if (!isAdmin && !user.getKeycloakSub().equals(authentication.getName())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).<Void>build();
            }

            // Update Keycloak
            keycloakAdminService.updateUserProfile(user.getKeycloakSub(), request);
            return ResponseEntity.noContent().<Void>build();
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private UserResponse toDto(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        
        // Basic split logic for display name to first/last name
        String display = u.getDisplayName();
        String first = display;
        String last = "";
        
        if (display != null && display.contains(" ")) {
            int idx = display.lastIndexOf(" ");
            first = display.substring(0, idx);
            last = display.substring(idx + 1);
        }
        
        r.setFirstName(first);
        r.setLastName(last);
        r.setEmail(u.getEmail());
        r.setPhotoUrl(u.getProfilePictureUrl());
        r.setRole("ROLE_USER"); 
        r.setActive(u.getStatus() == com.tigtech.persfinance.domain.UserStatus.ACTIVE);
        return r;
    }
}
