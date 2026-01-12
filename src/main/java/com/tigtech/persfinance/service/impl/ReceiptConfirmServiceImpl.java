package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.domain.*;
import com.tigtech.persfinance.repository.TransactionRepository;
import com.tigtech.persfinance.repository.WalletMemberRepository;
import com.tigtech.persfinance.repository.WalletRepository;
import com.tigtech.persfinance.service.ReceiptConfirmService;
import com.tigtech.persfinance.web.dto.ConfirmReceiptItem;
import com.tigtech.persfinance.web.dto.ConfirmReceiptRequest;
import com.tigtech.persfinance.web.dto.ConfirmReceiptResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;

@Service
@RequiredArgsConstructor
public class ReceiptConfirmServiceImpl implements ReceiptConfirmService {

    private final WalletRepository walletRepository;
    private final WalletMemberRepository walletMemberRepository;
    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public ConfirmReceiptResponse confirm(User user, ConfirmReceiptRequest request) {
        
        // 1. Validate total match
        BigDecimal itemsSum = request.getItems().stream()
                .map(ConfirmReceiptItem::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Normalize scale for comparison
        if (itemsSum.compareTo(request.getTotalAmount()) != 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                String.format("Sum of items (%s) does not match total amount (%s)", itemsSum, request.getTotalAmount()));
        }

        // 2. Validate wallet access
        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        WalletMember member = walletMemberRepository.findByWalletIdAndUserId(wallet.getId(), user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        if (member.getRole() == WalletRole.READER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions to create transaction");
        }

        // 3. Create parent transaction (EXPENSE)
        Transaction tx = Transaction.builder()
                .type(TransactionType.EXPENSE)
                .status(TransactionStatus.POSTED)
                .amount(request.getTotalAmount())
                .currency(request.getCurrency()) // "PLN" default
                .transactionDate(request.getTransactionDate())
                .merchant(request.getMerchant())
                .description(request.getDescription())
                .category("MIXED_PRODUCTS") // As per convention
                .isItemized(true)
                .wallet(wallet)
                .createdBy(user)
                .items(new ArrayList<>())
                .build();
        
        // 4. Create items
        for (ConfirmReceiptItem itemDto : request.getItems()) {
            TransactionItem item = TransactionItem.builder()
                    .transaction(tx)
                    .name(itemDto.getName())
                    .category(itemDto.getCategory())
                    .amount(itemDto.getAmount())
                    .build();
            tx.getItems().add(item);
        }

        tx = transactionRepository.save(tx);

        // 5. Update wallet balance
        BigDecimal current = wallet.getCurrentBalance() != null ? wallet.getCurrentBalance() : BigDecimal.ZERO;
        wallet.setCurrentBalance(current.subtract(tx.getAmount()));
        walletRepository.save(wallet);

        return ConfirmReceiptResponse.builder()
                .transactionId(tx.getId())
                .build();
    }
}
