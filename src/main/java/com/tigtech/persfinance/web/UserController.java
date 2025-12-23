package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.storage.StorageService;
import com.tigtech.persfinance.web.dto.UserResponse;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/api/users", produces = MediaType.APPLICATION_JSON_VALUE)
public class UserController {

    private final UserRepository userRepository;
    private final StorageService storageService;

    public UserController(UserRepository userRepository, StorageService storageService) {
        this.userRepository = userRepository;
        this.storageService = storageService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<UserResponse> list() {
        return userRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or #id == principal.id")
    public ResponseEntity<UserResponse> get(@PathVariable Long id) {
        return userRepository.findById(id).map(u -> ResponseEntity.ok(toDto(u))).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/photo")
    public ResponseEntity<?> uploadPhoto(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        return userRepository.findById(id).map(user -> {
            try {
                String url = storageService.uploadUserPhoto(file, String.valueOf(user.getId()));
                user.setPhotoUrl(url);
                userRepository.save(user);
                return ResponseEntity.ok().body(new UserResponse());
            } catch (Exception e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Upload failed"));
            }
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    private UserResponse toDto(User u) {
        UserResponse r = new UserResponse();
        r.setId(u.getId());
        r.setNome(u.getFirstName());
        r.setSobrenome(u.getLastName());
        r.setEmail(u.getEmail());
        r.setFotoUrl(u.getPhotoUrl());
        r.setRole(u.getRole());
        r.setAtivo(u.isActive());
        return r;
    }
}
