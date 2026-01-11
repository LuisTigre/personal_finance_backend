package com.tigtech.persfinance.web.dto;

import com.tigtech.persfinance.domain.WalletRole;
import lombok.Builder;
import lombok.Data;
import java.util.UUID;

@Data
@Builder
public class WalletMemberResponse {
    private UUID userId;
    private String email;
    private String displayName;
    private WalletRole role;
}
