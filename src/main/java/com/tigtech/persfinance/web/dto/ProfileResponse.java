package com.tigtech.persfinance.web.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProfileResponse {
    private String userId;
    private String firstName;
    private String lastName;
    private String email;
}
