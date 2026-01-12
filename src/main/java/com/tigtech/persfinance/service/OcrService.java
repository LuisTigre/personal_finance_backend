package com.tigtech.persfinance.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;

public interface OcrService {
    String extractText(MultipartFile file) throws IOException;
}
