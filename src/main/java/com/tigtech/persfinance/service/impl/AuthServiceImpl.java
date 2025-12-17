package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.domain.PasswordResetToken;
import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.mail.EmailService;
import com.tigtech.persfinance.repository.PasswordResetTokenRepository;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.AuthService;
import com.tigtech.persfinance.web.dto.ForgotPasswordRequest;
import com.tigtech.persfinance.web.dto.ResetPasswordRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public AuthServiceImpl(UserRepository userRepository, PasswordResetTokenRepository tokenRepository, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) return;
        User user = userOpt.get();
        String token = UUID.randomUUID().toString();
        PasswordResetToken prt = PasswordResetToken.of(
                token,
                user,
                LocalDateTime.now().plusHours(2)
        );
        tokenRepository.save(prt);
        String resetUrl = System.getenv().getOrDefault("APP_CLIENT_BASE_URL", "http://localhost:3000") + "/reset?token=" + token;
        emailService.sendPasswordReset(user.getEmail(), token, resetUrl);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        Optional<PasswordResetToken> t = tokenRepository.findByToken(request.getToken());
        if (t.isEmpty()) return;
        PasswordResetToken prt = t.get();
        if (prt.isExpired()) return;
        User user = prt.getUser();
        user.setSenha(passwordEncoder.encode(request.getNovaSenha()));
        userRepository.save(user);
        tokenRepository.delete(prt);
    }
}
