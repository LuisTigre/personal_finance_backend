package com.tigtech.persfinance.web.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponse {
    private Long id;
    private String nome;
    private String sobrenome;
    private String email;
    private String fotoUrl;
    private String role;
    private boolean ativo;
}

