package com.tigtech.persfinance.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ForgotPasswordRequest {
    @Email
    @NotBlank
    private String email;

    // explicit getter to ensure method exists even if Lombok is not processed
    public String getEmail() {
        return this.email;
    }
}
