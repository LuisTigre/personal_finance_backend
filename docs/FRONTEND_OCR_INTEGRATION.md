# Frontend Integration Guide: Receipt OCR & Confirmation

This guide outlines the integration steps for the new "Scan Receipt" feature. This feature allows users to upload an image of a receipt, review the OCR-extracted data, and save it as an **Itemized Transaction**.

## 1. Feature Workflow

1.  **Upload**: User selects an image file (receipt).
2.  **Draft**: Frontend uploads the file to `POST /api/receipts/ocr`.
3.  **Review & Enrich**: Frontend displays the draft data. 
    *   **CRITICAL**: The user **MUST** assign categories to items manually (OCR does not support categorization).
    *   User verifies amounts and merchant name.
4.  **Confirm**: User saves the receipt. Frontend calls `POST /api/receipts/confirm`.

---

## 2. API Contract

### Step 1: OCR Scan (Draft)

*   **Endpoint:** `POST /api/receipts/ocr`
*   **Content-Type:** `multipart/form-data`
*   **Authentication:** `Bearer <token>` (Any authenticated user)

#### Request
*   **Field Name:** `file` (The image file)

#### Response (Success 200)
```json
{
  "ocrText": "RAW OCR OUTPUT TEXT...",
  "merchant": "Biedronka",          // Best guess or null
  "totalAmount": 123.45,            // Best guess or null
  "currency": "PLN",
  "items": [
    {
      "name": "BANANY LUZ",
      "amount": 6.99,
      "category": null              // Always null from OCR
    },
    {
        "name": "MLEKO 3.2%",
        "amount": 3.49,
        "category": null
    }
  ],
  "warnings": ["Could not detect total amount"] // Check specific warnings
}
```

---

### Step 2: Confirm & Save (Finalize)

*   **Endpoint:** `POST /api/receipts/confirm`
*   **Content-Type:** `application/json`
*   **Authentication:** `Bearer <token>` (Requires WRITE or OWNER permissions on the wallet)

#### Request Payload
**Constraint:** `Sum(items.amount)` MUST strictly equal `totalAmount`. The backend will return `400 Bad Request` if there is a mismatch.

```json
{
  "walletId": "UUID-OF-TARGET-WALLET", 
  "transactionDate": "2026-01-12T12:00:00Z", // User configured date
  "merchant": "Biedronka",
  "description": "Weekly groceries",         // Optional
  "totalAmount": 10.48,                      // Must match sum of items
  "currency": "PLN",
  "items": [
    {
      "name": "BANANY LUZ",
      "amount": 6.99,
      "category": "GROCERIES"     // REQUIRED: User must pick this from dropdown
    },
    {
      "name": "MLEKO 3.2%",
      "amount": 3.49,
      "category": "DAIRY"
    }
  ]
}
```

#### Response (Success 200)
```json
{
  "transactionId": "550e8400-e29b-41d4-a716-446655440000"
}
```

---

## 3. Implementation Recommendations for Frontend Agent

1.  **Draft Review UI**:
    *   Show the "Raw OCR Text" in a collapsible section for debugging/verification.
    *   Render the items list as an editable table.
    *   **Category Dropdown**: Since OCR returns `null` for category, display a prominent "Select Category" dropdown for each item. Disable the "Confirm" button until all items have categories.
2.  **Total Validation**:
    *   If the user edits an item's price, update the visual "Sum of Items".
    *   Show a warning if "Sum of Items" != "Receipt Total".
3.  **Date Handling**:
    *   Pre-fill `transactionDate` with `new Date()` but allow the user to backdate it.
4.  **Error Handling**:
    *   `400 Bad Request`: Display the error message (e.g., "Sum of items (10.00) does not match total amount (12.00)").
    *   `403 Forbidden`: "You do not have permission to add transactions to this wallet."
