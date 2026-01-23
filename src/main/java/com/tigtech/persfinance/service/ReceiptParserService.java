package com.tigtech.persfinance.service;

import com.tigtech.persfinance.service.dto.OcrResult;
import com.tigtech.persfinance.web.dto.ReceiptOcrDraftResponse;

public interface ReceiptParserService {
    ReceiptOcrDraftResponse parse(String ocrText);
    
    // New overloaded method for structured result
    default ReceiptOcrDraftResponse parse(OcrResult result) {
        return parse(result.getFullText());
    }
}
