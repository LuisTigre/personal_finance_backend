package com.tigtech.persfinance.web.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ProfileUpdateRequest {
    @Size(max = 60)
    private String firstName;
    
    @Size(max = 60)
    private String lastName;
}
