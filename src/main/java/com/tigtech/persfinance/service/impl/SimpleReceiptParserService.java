package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.service.ReceiptParserService;
import com.tigtech.persfinance.service.dto.OcrResult;
import com.tigtech.persfinance.web.dto.ReceiptDraftItem;
import com.tigtech.persfinance.web.dto.ReceiptOcrDraftResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@Slf4j
public class SimpleReceiptParserService implements ReceiptParserService {

    private static final List<String> KNOWN_MERCHANTS = Arrays.asList("BIEDRONKA", "LIDL", "AUCHAN", "CARREFOUR", "ZABKA", "ROSSMANN", "HEBE");
    private static final List<String> TOTAL_KEYWORDS = Arrays.asList("SUMA", "RAZEM", "DO ZAPŁATY", "TOTAL");
    private static final List<String> SKIP_KEYWORDS = Arrays.asList("SPRZEDAŻ", "PTU", "STAWKA", "NETTO", "BRUTTO", "KARTA", "GOTÓWKA", "RESZTA", "NIP", "PARAGON", "FISKALNY", "BD0", "CENA", "ZŁ", "PLN");
    
    // Regex for Polish currency format: 12,34 or 12.34 or -12,34
    // Using loose boundaries to catch numbers even if stuck to text
    private static final Pattern VALUE_PATTERN = Pattern.compile("-?\\d+[.,]\\d{2}");

    @Override
    public ReceiptOcrDraftResponse parse(String ocrText) {
         // Fallback legacy implementation
         return parseLegacy(ocrText);
    }
    
    @Override
    public ReceiptOcrDraftResponse parse(OcrResult result) {
        if (result.getLines() == null || result.getLines().isEmpty()) {
            return parseLegacy(result.getFullText());
        }
        
        List<String> warnings = new ArrayList<>();
        List<ReceiptDraftItem> items = new ArrayList<>();
        
        // 1. Line Analysis
        List<OcrResult.OcrLine> lines = result.getLines();
        
        // DEDUPLICATION: Check for possible duplicate lines if we have multiple pages
        // This is a simple heuristic: if a block of 3+ lines repeats exactly (content+value), ignore subsequent occurrences.
        // However, user receipts might actually have identical items.
        // Better strategy: Filter out lines that are excessively repetitive if they come from different pages.
        // For now, let's just make sure we don't parse "Page 1 matches Page 2" duplicates.
        // We can do this by tracking which page indices are "valid" by asking Service to mark duplicates? 
        // Or simply:
        
        String merchant = guessMerchant(lines.stream().map(OcrResult.OcrLine::getText).toArray(String[]::new));
        
        BigDecimal grandTotalDetected = null;
        BigDecimal runningTotal = BigDecimal.ZERO;
        
        // Create a unique signature for deduplication <normalized_text>
        // But only dedupe if we suspect page duplication.
        // Since we don't have the "isDuplicate" flag from OcrResult easily available here (it's logged but not passed in OcrResult structure explicitly unless we add it),
        // we will deduplicate lines that are "exactly the same text".
        // Wait, standard receipts often have "1 x 2.00   2.00".
        
        for (OcrResult.OcrLine line : lines) {
            String text = line.getText().toUpperCase();
            
            // Skip structural/header lines
            if (shouldSkip(text)) continue;
             
            // Detect Grand Total Row
            if (isTotalRow(text)) {
                 BigDecimal val = extractRightMostValue(line);
                 // If we find multiple "SUMA", we likely want the last one or the largest one? 
                 // Or if it's a duplicate, it's just the same summary repeated.
                 if (val != null) grandTotalDetected = val;
                 continue; 
            }
            
            // Detect "Rabat" / Discount
            boolean isDiscount = text.contains("RABAT") || text.contains("UPUST") || text.contains("DISK");
            
            // Extract Amount (Right-most value strategy)
            BigDecimal lineAmount = extractRightMostValue(line);
            
            if (lineAmount != null) {
                // If it's a discount line, ensure amount is negative
                if (isDiscount && lineAmount.compareTo(BigDecimal.ZERO) > 0) {
                    lineAmount = lineAmount.negate();
                }
                
                // Construct Item Name (Everything to the left of the value)
                String name = extractNameFromLine(line, lineAmount);
                if (name.length() < 2) continue; // Noise
                
                String category = isDiscount ? "Discount" : "Uncategorized";
                
                // inherit category if this is a discount for prev item (heuristic)
                if (isDiscount && !items.isEmpty()) {
                    category = items.get(items.size() - 1).getCategory();
                }

                items.add(ReceiptDraftItem.builder()
                        .name(name)
                        .amount(lineAmount)
                        .category(category)
                        .build());
                        
                runningTotal = runningTotal.add(lineAmount);
            }
        }
        
        // Validation with Deduplication Fallback
        // If the Computed Total is roughly N * Detected Total (where N is integer > 1)
        // Then we likely have page duplication.
        if (grandTotalDetected != null && runningTotal.compareTo(grandTotalDetected) > 0) {
             BigDecimal multiple = runningTotal.divide(grandTotalDetected, 1, java.math.RoundingMode.HALF_UP);
             // if multiple is close to 2.0 or 3.0
             double mult = multiple.doubleValue();
             if (Math.abs(mult - Math.round(mult)) < 0.1 && Math.round(mult) >= 2) {
                 warnings.add("Detected possible duplicate receipt processing (Total matches " + Math.round(mult) + "x document sum). Keeping only one set.");
                 // Heuristic: Slice the items list
                 int correctSize = items.size() / (int)Math.round(mult);
                 items = items.subList(0, correctSize);
                 // Recompute running total
                 runningTotal = items.stream().map(ReceiptDraftItem::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
             }
        }
        
        boolean needsReview = false;
        BigDecimal diff = null;
        
        if (grandTotalDetected != null) {
            diff = grandTotalDetected.subtract(runningTotal);
        if (diff.abs().compareTo(new BigDecimal("0.50")) > 0) {
                needsReview = true;
                warnings.add("Calculated total (" + runningTotal + ") differs from detected total (" + grandTotalDetected + ")");
            }
        } else {
             warnings.add("Could not detect receipt total (SUMA)");
             grandTotalDetected = runningTotal; 
        }

        if (merchant == null) warnings.add("Could not detect merchant");

        return ReceiptOcrDraftResponse.builder()
                .fullText(result.getFullText())
                .ocrText(result.getFullText())
                .merchant(merchant)
                .totalAmount(grandTotalDetected)
                .computedTotal(runningTotal)
                .detectedTotal(grandTotalDetected)
                .diffTotal(diff)
                .needsReview(needsReview)
                .currency("PLN")
                .items(items)
                .warnings(warnings)
                .build();
    }
    
    private boolean shouldSkip(String text) {
        // Must match at least one SKIP keyword AND not look like a regular item
        // But headers like "SPRZEDAŻ OPODATKOWANA" etc usually contain these words
        // Be careful not to skip items like "SEREK" (contains nothing bad)
        // Check for specific footer keywords
        if (text.contains("NIP") || text.contains("REGON") || text.contains("KASJER")) return true;
        
        // Skip lines that are purely totals breakdown 
        if (text.startsWith("PTU") || text.startsWith("SP.OP")) return true;

        for (String skipKw : SKIP_KEYWORDS) {
            if (text.contains(skipKw)) return true;
        }
        
        return false;
    }
    
    private boolean isTotalRow(String text) {
        for (String kw : TOTAL_KEYWORDS) {
            if (text.contains(kw)) return true;
        }
        return false;
    }

    private BigDecimal extractRightMostValue(OcrResult.OcrLine line) {
        // Iterate tokens from right to left
        List<OcrResult.OcrToken> tokens = line.getTokens();
        if (tokens != null && !tokens.isEmpty()) {
            for (int i = tokens.size() - 1; i >= 0; i--) {
                String t = tokens.get(i).getText();
                Matcher m = VALUE_PATTERN.matcher(t);
                if (m.find()) {
                    // If token contains a number, parse it.
                    // handle "12,99A" or "A12,99"
                    return parseAmount(m.group());
                }
            }
        }
        
        // Fallback: search in text from right (if tokens missing or no match in tokens)
        if (line.getText() != null) {
            Matcher m = VALUE_PATTERN.matcher(line.getText());
            String lastMatch = null;
            while (m.find()) lastMatch = m.group();
            if (lastMatch != null) return parseAmount(lastMatch);
        }
        
        return null; // No number found
    }
    
    // Fallback if tokens not available or perfectly aligned, operate on text
    private String extractNameFromLine(OcrResult.OcrLine line, BigDecimal amountVal) {
        // Remove the tokens used for price? Or simple text replacement?
        // Simple: remove the amount string from the end of text
        String lineText = line.getText();
        // Remove amount pattern roughly
        String valStr1 = amountVal.toString().replace(".", ",");
        String valStr2 = amountVal.toString();
        
        // Try to locate index of price
        int idx = lineText.lastIndexOf(valStr1);
        if (idx == -1) idx = lineText.lastIndexOf(valStr2);
        
        if (idx > 0) {
            return lineText.substring(0, idx).trim();
        }
        return lineText.replace(valStr1, "").replace(valStr2, "").trim(); 
    }

    private ReceiptOcrDraftResponse parseLegacy(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            return ReceiptOcrDraftResponse.builder()
                    .ocrText("")
                    .items(List.of())
                    .warnings(new ArrayList<>(List.of("Empty OCR text")))
                    .build();
        }

        String[] lines = ocrText.split("\\r?\\n");
        List<String> warnings = new ArrayList<>();
        
        String merchant = guessMerchant(lines);
        BigDecimal totalAmount = guessTotal(lines);
        List<ReceiptDraftItem> items = guessItems(lines);
        
        // ... (rest of legacy logic) ...
        // Re-use legacy methods
        
        if (merchant == null) warnings.add("Could not detect merchant");
        if (totalAmount == null) warnings.add("Could not detect total amount");

        return ReceiptOcrDraftResponse.builder()
                .ocrText(ocrText)
                .merchant(merchant)
                .totalAmount(totalAmount)
                .currency("PLN")
                .items(items)
                .warnings(warnings)
                .build();
    }
    
   // ... keep existing private methods (guessMerchant, guessTotal, guessItems) ...
   // COPYING THEM BACK CAREFULLY

    private String guessMerchant(String[] lines) {
        for (String line : lines) {
            String upper = line.toUpperCase();
            for (String known : KNOWN_MERCHANTS) {
                if (upper.contains(known)) return known;
            }
        }
        for (String line : lines) {
            if (line.trim().length() > 3) return line.trim();
        }
        return null;
    }

    private BigDecimal guessTotal(String[] lines) {
        for (int i = lines.length - 1; i >= 0; i--) {
            String line = lines[i].toUpperCase();
            for (String keyword : TOTAL_KEYWORDS) {
                if (line.contains(keyword)) {
                   return extractAmountLegacy(lines[i]); // calling legacy helper
                }
            }
        }
        return null;
    }
    
    private List<ReceiptDraftItem> guessItems(String[] lines) {
         // Legacy regex based
        List<ReceiptDraftItem> items = new ArrayList<>();
        Pattern legacyItemPattern = Pattern.compile("^(.+?)\\s+(\\d+[.,]\\d{2})\\s*[A-Zmpt]*$");
        
        for (String line : lines) {
            boolean isTotalLine = false;
            for (String kw : TOTAL_KEYWORDS) {
                if (line.toUpperCase().contains(kw)) { isTotalLine = true; break; }
            }
            if (isTotalLine) continue;

            Matcher m = legacyItemPattern.matcher(line.trim());
            if (m.find()) {
                try {
                    String name = m.group(1).trim();
                    BigDecimal amount = parseAmount(m.group(2));
                    if (name.length() > 2 && amount.compareTo(BigDecimal.ZERO) > 0) {
                        items.add(ReceiptDraftItem.builder().name(name).amount(amount).category("Uncategorized").build());
                    }
                } catch (Exception ignored) {}
            }
        }
        return items;
    }
    
    private BigDecimal extractAmountLegacy(String line) {
        Matcher m = VALUE_PATTERN.matcher(line);
        String lastMatch = null;
        while (m.find()) lastMatch = m.group();
        return parseAmount(lastMatch);
    }

    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null) return null;
        return new BigDecimal(amountStr.replace(",", "."));
    }
}
