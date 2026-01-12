package com.tigtech.persfinance.service;

import com.tigtech.persfinance.web.dto.ReceiptOcrDraftResponse;

public interface ReceiptParserService {
    ReceiptOcrDraftResponse parse(String ocrText);
}
