package com.tigtech.persfinance.web.dto;

import lombok.Getter;
import lombok.Setter;
import java.util.UUID;

@Getter
@Setter
public class UserResponse {
    private UUID id;
    private String firstName;
    private String lastName;
    private String email;
    private String photoUrl;
    private String role;
    private boolean active;
}

