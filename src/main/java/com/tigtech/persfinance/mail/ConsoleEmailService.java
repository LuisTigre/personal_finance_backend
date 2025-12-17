package com.tigtech.persfinance.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ConsoleEmailService implements EmailService {

    private static final Logger log = LoggerFactory.getLogger(ConsoleEmailService.class);

    @Override
    public void sendPasswordReset(String toEmail, String resetToken, String resetUrl) {
        String message = String.format("Password reset requested for %s. Token: %s. Reset URL: %s", toEmail, resetToken, resetUrl);
        log.info(message);
        System.out.println(message);
    }
}

