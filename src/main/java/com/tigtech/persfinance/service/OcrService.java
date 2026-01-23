package com.tigtech.persfinance.service;

import com.tigtech.persfinance.service.dto.OcrResult;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface OcrService {
    String extractText(MultipartFile file) throws IOException;
    
    OcrResult processUpload(MultipartFile file) throws IOException;
}
