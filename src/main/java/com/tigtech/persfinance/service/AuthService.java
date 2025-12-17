package com.tigtech.persfinance.service;

import com.tigtech.persfinance.web.dto.ForgotPasswordRequest;
import com.tigtech.persfinance.web.dto.ResetPasswordRequest;

public interface AuthService {
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
}

