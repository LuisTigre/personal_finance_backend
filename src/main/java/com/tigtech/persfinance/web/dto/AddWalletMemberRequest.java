package com.tigtech.persfinance.web.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.tigtech.persfinance.domain.WalletRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddWalletMemberRequest {
    @JsonAlias({"email", "userId", "username"})
    @NotBlank
    private String identifier; // email OR userId OR username
    
    @NotNull
    private WalletRole role = WalletRole.WRITER;
}
