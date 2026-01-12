package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.OcrService;
import com.tigtech.persfinance.service.ReceiptConfirmService;
import com.tigtech.persfinance.service.ReceiptParserService;
import com.tigtech.persfinance.web.dto.ConfirmReceiptRequest;
import com.tigtech.persfinance.web.dto.ConfirmReceiptResponse;
import com.tigtech.persfinance.web.dto.ReceiptOcrDraftResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;

@RestController
@RequestMapping(value = "/api/receipts", produces = MediaType.APPLICATION_JSON_VALUE)
@RequiredArgsConstructor
public class ReceiptController {

    private final OcrService ocrService;
    private final ReceiptParserService receiptParserService;
    private final ReceiptConfirmService receiptConfirmService;
    private final UserRepository userRepository;

    @PostMapping(value = "/ocr", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ReceiptOcrDraftResponse ocrDraft(@RequestParam("file") MultipartFile file, JwtAuthenticationToken principal) {
        // Check user exists (any role can try OCR)
        getUser(principal);

        try {
            String ocrText = ocrService.extractText(file);
            return receiptParserService.parse(ocrText);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "OCR processing failed: " + e.getMessage());
        }
    }

    @PostMapping("/confirm")
    public ConfirmReceiptResponse confirm(@Valid @RequestBody ConfirmReceiptRequest request, JwtAuthenticationToken principal) {
        User user = getUser(principal);
        return receiptConfirmService.confirm(user, request);
    }

    private User getUser(JwtAuthenticationToken principal) {
        if (principal == null || principal.getToken() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing or invalid JWT");
        }
        String sub = principal.getToken().getSubject();
        return userRepository.findByKeycloakSub(sub)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not registered in system"));
    }
}
