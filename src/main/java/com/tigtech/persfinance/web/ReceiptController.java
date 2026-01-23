package com.tigtech.persfinance.web;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.repository.UserRepository;
import com.tigtech.persfinance.service.OcrService;
import com.tigtech.persfinance.service.ReceiptConfirmService;
import com.tigtech.persfinance.service.ReceiptParserService;
import com.tigtech.persfinance.service.dto.OcrResult;
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
import java.util.List;
import java.util.stream.Collectors;

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
            OcrResult ocrResult = ocrService.processUpload(file);
            
            // Fail-safe Guard (Req 4): Do not finalize totals if OCR incomplete
            boolean isComplete = ocrResult.getStatus() == OcrResult.OcrStatus.SUCCESS;
            
            ReceiptOcrDraftResponse response;
            if (isComplete) {
                // Use the new overloaded parse method that accepts OcrResult
                response = receiptParserService.parse(ocrResult);
            } else {
                // Return barebones response with error status
                response = ReceiptOcrDraftResponse.builder()
                        .ocrText(ocrResult.getFullText())
                        .fullText(ocrResult.getFullText())
                        .items(List.of())
                        .warnings(List.of("OCR Incomplete or Failed: " + ocrResult.getStatus()))
                        .build();
            }
            
            // Enrich response with metadata (Req 8)
            response.setFileType(ocrResult.getFileType());
            response.setPdfPageCount(ocrResult.getTotalPageCount());
            response.setOcrProcessedPages(ocrResult.getProcessedPageCount());
            response.setStatus(ocrResult.getStatus().name());

            // Enrich response with pages
            if (ocrResult.getPages() != null) {
                response.setPages(ocrResult.getPages().stream()
                        .map(page -> ReceiptOcrDraftResponse.PageOcrResult.builder()
                                .pageIndex(page.getPageIndex())
                                .text(page.getText())
                                .build())
                        .collect(Collectors.toList()));
            }
            
            return response;

        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error processing file", e);
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
