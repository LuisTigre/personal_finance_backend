package com.tigtech.persfinance.service;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.web.dto.ConfirmReceiptRequest;
import com.tigtech.persfinance.web.dto.ConfirmReceiptResponse;

public interface ReceiptConfirmService {
    ConfirmReceiptResponse confirm(User user, ConfirmReceiptRequest request);
}
