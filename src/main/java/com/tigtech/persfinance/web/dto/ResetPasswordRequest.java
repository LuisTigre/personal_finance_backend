package com.tigtech.persfinance.web.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResetPasswordRequest {
    @NotBlank
    private String token;

    @NotBlank
    private String novaSenha;

    public String getToken() {
        return this.token;
    }

    public String getNovaSenha() {
        return this.novaSenha;
    }
}
