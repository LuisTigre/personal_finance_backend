package com.tigtech.persfinance.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "users", uniqueConstraints = @UniqueConstraint(columnNames = "email"))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password", nullable = true)
    private String password; // BCrypt hashed password

    @Column(nullable = false)
    @Builder.Default
    private String provider = "local"; // local or google

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "photo_url")
    private String photoUrl;

    @Column(nullable = false)
    @Builder.Default
    private String role = "ROLE_USER";

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Backward-compatible accessors for legacy Portuguese names used elsewhere.
    // Marked with @JsonIgnore so they are not serialized in API responses.
    @JsonIgnore
    public String getPassword() {
        return this.password;
    }

    @JsonIgnore
    public void setPassword(String password) {
        this.password = password;
    }

    @JsonIgnore
    public boolean isActive() {
        return this.active;
    }

    @JsonIgnore
    public boolean getActive() {
        return this.active;
    }
}
