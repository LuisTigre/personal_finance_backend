package com.tigtech.persfinance.web.dto;

import com.tigtech.persfinance.domain.WalletStatus;
import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class WalletResponse {
    private UUID id;
    private String name;
    private String currency;
    private BigDecimal currentBalance;
    private WalletStatus status;
    private Instant createdAt;
    private List<WalletMemberResponse> members;
}
