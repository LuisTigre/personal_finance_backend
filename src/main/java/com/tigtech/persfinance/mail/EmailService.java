package com.tigtech.persfinance.mail;

public interface EmailService {
    void sendPasswordReset(String toEmail, String resetToken, String resetUrl);
}

