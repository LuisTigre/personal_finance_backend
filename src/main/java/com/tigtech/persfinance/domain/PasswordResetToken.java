package com.tigtech.persfinance.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }

    // Static factory to avoid direct dependence on Lombok's builder() in environments where
    // Lombok annotation processing might not run. Use PasswordResetToken.of(token,user,expiresAt)
    // as a compact alternative to builder().
    public static PasswordResetToken of(String token, User user, LocalDateTime expiresAt) {
        PasswordResetToken prt = new PasswordResetToken();
        prt.token = token;
        prt.user = user;
        prt.expiresAt = expiresAt;
        return prt;
    }

    // Explicit getter for user to ensure method exists if Lombok isn't processed
    public User getUser() {
        return this.user;
    }
}
