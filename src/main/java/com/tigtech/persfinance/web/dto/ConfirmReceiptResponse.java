package com.tigtech.persfinance.web.dto;

import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class ConfirmReceiptResponse {
    private UUID transactionId;
}
