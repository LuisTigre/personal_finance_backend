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

    @Column(name = "nome", nullable = false)
    private String firstName;

    @Column(name = "sobrenome", nullable = false)
    private String lastName;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "senha", nullable = true)
    private String password; // BCrypt hashed password

    @Column(nullable = false)
    private String provider = "local"; // local or google

    @Column(name = "provider_id")
    private String providerId;

    @Column(name = "foto_url")
    private String fotoUrl;

    @Column(nullable = false)
    private String role = "ROLE_USER";

    @Column(name = "ativo", nullable = false)
    private boolean active = true;

    @Column(name = "data_criacao", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "data_atualizacao")
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
    public String getSenha() {
        return this.password;
    }

    @JsonIgnore
    public void setSenha(String senha) {
        this.password = senha;
    }

    @JsonIgnore
    public boolean isAtivo() {
        return this.active;
    }

    @JsonIgnore
    public boolean getAtivo() {
        return this.active;
    }
}
