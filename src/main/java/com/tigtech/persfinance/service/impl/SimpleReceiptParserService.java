package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.service.ReceiptParserService;
import com.tigtech.persfinance.web.dto.ReceiptDraftItem;
import com.tigtech.persfinance.web.dto.ReceiptOcrDraftResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@Slf4j
public class SimpleReceiptParserService implements ReceiptParserService {

    private static final List<String> KNOWN_MERCHANTS = Arrays.asList("BIEDRONKA", "LIDL", "AUCHAN", "CARREFOUR", "ZABKA", "ROSSMANN", "HEBE");
    private static final List<String> TOTAL_KEYWORDS = Arrays.asList("SUMA", "RAZEM", "DO ZAPŁATY", "TOTAL");
    
    // Matches line ending with price: "ITEM NAME 12,34" or "ITEM NAME 12.34"
    // Capture group 1: Name, Group 2: Amount string
    private static final Pattern ITEM_PATTERN = Pattern.compile("^(.+?)\\s+(\\d+[.,]\\d{2})\\s*[A-Zmpt]*$");

    @Override
    public ReceiptOcrDraftResponse parse(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return ReceiptOcrDraftResponse.builder()
                    .ocrText("")
                    .items(List.of())
                    .warnings(List.of("Empty OCR text"))
                    .build();
        }

        String[] lines = ocrText.split("\\r?\\n");
        List<String> warnings = new ArrayList<>();
        
        String merchant = guessMerchant(lines);
        BigDecimal totalAmount = guessTotal(lines);
        List<ReceiptDraftItem> items = guessItems(lines);
        
        if (merchant == null) warnings.add("Could not detect merchant");
        if (totalAmount == null) warnings.add("Could not detect total amount");
        if (items.isEmpty()) warnings.add("Could not detect any items");

        return ReceiptOcrDraftResponse.builder()
                .ocrText(ocrText)
                .merchant(merchant)
                .totalAmount(totalAmount)
                .currency("PLN") // Default
                .items(items)
                .warnings(warnings)
                .build();
    }

    private String guessMerchant(String[] lines) {
        for (String line : lines) {
            String upper = line.toUpperCase();
            for (String known : KNOWN_MERCHANTS) {
                if (upper.contains(known)) return known;
            }
        }
        // Fallback: first non-empty line that looks like text
        for (String line : lines) {
            if (line.trim().length() > 3) return line.trim();
        }
        return null;
    }

    private BigDecimal guessTotal(String[] lines) {
        // Search from bottom up for total
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].toUpperCase();
            for (String keyword : TOTAL_KEYWORDS) {
                if (line.contains(keyword)) {
                    return extractAmount(lines[i]);
                }
            }
        }
        return null;
    }

    private List<ReceiptDraftItem> guessItems(String[] lines) {
        List<ReceiptDraftItem> items = new ArrayList<>();
        for (String line : lines) {
            // naive skip of total lines
            boolean isTotalLine = false;
            for (String kw : TOTAL_KEYWORDS) {
                if (line.toUpperCase().contains(kw)) {
                    isTotalLine = true; 
                    break;
                }
            }
            if (isTotalLine) continue;

            Matcher m = ITEM_PATTERN.matcher(line.trim());
            if (m.find()) {
                try {
                    String name = m.group(1).trim();
                    BigDecimal amount = parseAmount(m.group(2));
                    
                    // Simple filter to avoid noise
                    if (name.length() > 2 && amount.compareTo(BigDecimal.ZERO) > 0) {
                        items.add(ReceiptDraftItem.builder()
                                .name(name)
                                .amount(amount)
                                .build());
                    }
                } catch (Exception ignored) {}
            }
        }
        return items;
    }

    private BigDecimal extractAmount(String line) {
        // find last number pattern in line
        Pattern p = Pattern.compile("(\\d+[.,]\\d{2})");
        Matcher m = p.matcher(line);
        String lastMatch = null;
        while (m.find()) {
            lastMatch = m.group(1);
        }
        return lastMatch != null ? parseAmount(lastMatch) : null;
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null) return null;
        return new BigDecimal(amountStr.replace(",", "."));
    }
}
