package com.tigtech.persfinance.service.impl;

import com.tigtech.persfinance.domain.*;
import com.tigtech.persfinance.repository.TransactionRepository;
import com.tigtech.persfinance.repository.TransactionItemRepository;
import com.tigtech.persfinance.repository.WalletMemberRepository;
import com.tigtech.persfinance.repository.WalletRepository;
import com.tigtech.persfinance.service.TransactionService;
import com.tigtech.persfinance.web.dto.CreateTransactionRequest;
import com.tigtech.persfinance.web.dto.ListTransactionsResponse;
import com.tigtech.persfinance.web.dto.TransactionResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;
    private final WalletRepository walletRepository;
    private final WalletMemberRepository walletMemberRepository;
    private final TransactionItemRepository transactionItemRepository;

    @Override
    public TransactionResponse createTransaction(User user, CreateTransactionRequest request) {
        validateAmount(request.getAmount());

        return switch (request.getType()) {
            case EXPENSE -> createExpense(user, request);
            case INCOME -> createIncome(user, request);
            case TRANSFER -> createTransfer(user, request);
        };
    }

    @Override
    @Transactional(readOnly = true)
    public ListTransactionsResponse listTransactions(User user,
                                                    UUID walletId,
                                                    Instant from,
                                                    Instant to,
                                                    String type,
                                                    String q,
                                                    String status) {
        TransactionType parsedType = parseType(type);
        TransactionStatus parsedStatus = parseStatus(status);

        Instant fromDate = (from != null) ? from : Instant.EPOCH;
        Instant toDate = (to != null) ? to : Instant.ofEpochSecond(253402300799L); // 9999-12-31T23:59:59Z
        String query = (q != null) ? q.trim() : "";

        List<Transaction> txs;
        if (walletId != null && parsedType != null) {
            txs = transactionRepository.findAllForUserMembershipByWalletAndType(user.getId(), walletId, fromDate, toDate, parsedType, query, parsedStatus);
        } else if (walletId != null) {
            txs = transactionRepository.findAllForUserMembershipByWallet(user.getId(), walletId, fromDate, toDate, query, parsedStatus);
        } else if (parsedType != null) {
            txs = transactionRepository.findAllForUserMembershipByType(user.getId(), fromDate, toDate, parsedType, query, parsedStatus);
        } else {
            txs = transactionRepository.findAllForUserMembership(user.getId(), fromDate, toDate, query, parsedStatus);
        }

        BigDecimal totalIncome = BigDecimal.ZERO;
        BigDecimal totalExpense = BigDecimal.ZERO;

        for (Transaction t : txs) {
            if (t.getStatus() != TransactionStatus.POSTED) continue;
            if (t.getType() == TransactionType.INCOME) totalIncome = totalIncome.add(t.getAmount());
            if (t.getType() == TransactionType.EXPENSE) totalExpense = totalExpense.add(t.getAmount());
        }

        BigDecimal net = totalIncome.subtract(totalExpense);

        return ListTransactionsResponse.builder()
            .items(txs.stream().map(this::toResponse).toList())
            .totals(ListTransactionsResponse.Totals.builder()
                .totalIncome(totalIncome)
                .totalExpense(totalExpense)
                .net(net)
                .build())
            .build();
    }

    @Override
    public TransactionResponse softDeleteTransaction(User user, UUID transactionId) {
        Transaction tx = transactionRepository.findByIdAndUserHasAccess(transactionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (tx.getStatus() == TransactionStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Transaction already deleted");
        }

        // Permission check: WRITER/OWNER in relevant wallet(s)
        if (tx.getType() == TransactionType.TRANSFER) {
            requireWriteAccess(tx.getFromWallet().getId(), user.getId());
            requireWriteAccess(tx.getToWallet().getId(), user.getId());
        } else {
            requireWriteAccess(tx.getWallet().getId(), user.getId());
        }

        // Reverse balances
        applyBalanceImpact(tx, true);

        tx.setStatus(TransactionStatus.DELETED);
        tx = transactionRepository.save(tx);

        return toResponse(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public com.tigtech.persfinance.web.dto.TransactionDetailsResponse getTransactionDetails(User user, UUID transactionId) {
        Transaction tx = transactionRepository.findByIdAndUserHasAccess(transactionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        TransactionResponse base = toResponse(tx);
        
        List<com.tigtech.persfinance.web.dto.TransactionItemDto> items = new java.util.ArrayList<>();
        if (tx.getItems() != null) {
            items = tx.getItems().stream()
                    .map(i -> new com.tigtech.persfinance.web.dto.TransactionItemDto(
                            i.getId(), i.getName(), i.getCategory(), i.getAmount(), i.getNote()))
                    .toList();
        }

        BigDecimal allocated = items.stream()
                .map(com.tigtech.persfinance.web.dto.TransactionItemDto::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean isBalanced = !tx.isItemized() || allocated.compareTo(tx.getAmount()) == 0;

        return com.tigtech.persfinance.web.dto.TransactionDetailsResponse.builder()
                .transaction(base)
                .items(items)
                .allocatedTotal(allocated)
                .isBalanced(isBalanced)
                .build();
    }

    @Override
    public TransactionResponse replaceTransactionItems(User user, UUID transactionId, com.tigtech.persfinance.web.dto.ReplaceTransactionItemsRequest request) {
        Transaction tx = transactionRepository.findByIdAndUserHasAccess(transactionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (tx.getStatus() == TransactionStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify deleted transaction");
        }

        // Permission check
        if (tx.getType() == TransactionType.TRANSFER) {
            requireWriteAccess(tx.getFromWallet().getId(), user.getId());
            requireWriteAccess(tx.getToWallet().getId(), user.getId());
        } else {
            requireWriteAccess(tx.getWallet().getId(), user.getId());
        }

        BigDecimal totalItems = request.getItems().stream()
                .map(com.tigtech.persfinance.web.dto.ReplaceTransactionItemsRequest.ItemRequest::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalItems.compareTo(tx.getAmount()) != 0) {
            // Validation per prompt: enforce SUM(items) == tx.amount
             throw new ResponseStatusException(HttpStatus.BAD_REQUEST, 
                 "Sum of items (" + totalItems + ") does not match transaction amount (" + tx.getAmount() + ")");
        }

        // Clear existing items
        if (tx.getItems() != null) {
            tx.getItems().clear();
        } else {
            tx.setItems(new java.util.ArrayList<>());
        }

        // Add new items
        for (com.tigtech.persfinance.web.dto.ReplaceTransactionItemsRequest.ItemRequest itemReq : request.getItems()) {
            TransactionItem item = TransactionItem.builder()
                    .transaction(tx)
                    .name(itemReq.getName())
                    .category(itemReq.getCategory())
                    .amount(itemReq.getAmount())
                    .note(itemReq.getNote())
                    .build();
            tx.getItems().add(item);
        }

        tx.setItemized(true);
        // Prompt says: category should be NULL or "MIXED_PRODUCTS"
        // I'll set it to MIXED_PRODUCTS if it wasn't already specific, or just leave it?
        // "must not break old behavior" -> if I change it, I might break reporting.
        // Prompt: "category should be NULL or 'MIXED_PRODUCTS' (choose one approach but do NOT break old behavior)"
        // I'll opt to update it to MIXED_PRODUCTS to indicate it's a split transaction at high level.
        tx.setCategory("MIXED_PRODUCTS");

        tx = transactionRepository.save(tx);
        return toResponse(tx);
    }

    @Override
    public TransactionResponse clearTransactionItems(User user, UUID transactionId) {
        Transaction tx = transactionRepository.findByIdAndUserHasAccess(transactionId, user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        if (tx.getStatus() == TransactionStatus.DELETED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cannot modify deleted transaction");
        }

        // Permission check
        if (tx.getType() == TransactionType.TRANSFER) {
            requireWriteAccess(tx.getFromWallet().getId(), user.getId());
            requireWriteAccess(tx.getToWallet().getId(), user.getId());
        } else {
            requireWriteAccess(tx.getWallet().getId(), user.getId());
        }

        if (tx.getItems() != null) {
            tx.getItems().clear();
        }
        
        tx.setItemized(false);
        // Maybe revert category? Hard to know what it was. I'll leave it or set to null?
        // Prompt doesn't specify revert logic, just "sets is_itemized=false".
        
        tx = transactionRepository.save(tx);
        return toResponse(tx);
    }

    private TransactionResponse createExpense(User user, CreateTransactionRequest request) {
        if (request.getWalletId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "walletId is required for EXPENSE");
        }
        if (request.getFromWalletId() != null || request.getToWalletId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromWalletId/toWalletId must be null for EXPENSE");
        }

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        requireWriteAccess(wallet.getId(), user.getId());

        Transaction tx = Transaction.builder()
                .type(TransactionType.EXPENSE)
                .status(TransactionStatus.POSTED)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .transactionDate(request.getTransactionDate())
                .category(request.getCategory())
                .description(request.getDescription())
                .wallet(wallet)
                .createdBy(user)
                .build();

        tx = transactionRepository.save(tx);

        // Apply balance
        applyBalanceImpact(tx, false);

        return toResponse(tx);
    }

    private TransactionResponse createIncome(User user, CreateTransactionRequest request) {
        if (request.getWalletId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "walletId is required for INCOME");
        }
        if (request.getFromWalletId() != null || request.getToWalletId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromWalletId/toWalletId must be null for INCOME");
        }

        Wallet wallet = walletRepository.findById(request.getWalletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));

        requireWriteAccess(wallet.getId(), user.getId());

        Transaction tx = Transaction.builder()
                .type(TransactionType.INCOME)
                .status(TransactionStatus.POSTED)
                .amount(request.getAmount())
                .currency(wallet.getCurrency())
                .transactionDate(request.getTransactionDate())
                .category(request.getCategory())
                .description(request.getDescription())
                .wallet(wallet)
                .createdBy(user)
                .build();

        tx = transactionRepository.save(tx);

        // Apply balance
        applyBalanceImpact(tx, false);

        return toResponse(tx);
    }

    private TransactionResponse createTransfer(User user, CreateTransactionRequest request) {
        if (request.getWalletId() != null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "walletId must be null for TRANSFER");
        }
        if (request.getFromWalletId() == null || request.getToWalletId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromWalletId and toWalletId are required for TRANSFER");
        }
        if (request.getFromWalletId().equals(request.getToWalletId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "fromWalletId and toWalletId must be different");
        }

        Wallet fromWallet = walletRepository.findById(request.getFromWalletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "fromWallet not found"));
        Wallet toWallet = walletRepository.findById(request.getToWalletId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "toWallet not found"));

        if (!fromWallet.getCurrency().equalsIgnoreCase(toWallet.getCurrency())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer requires matching wallet currencies");
        }

        requireWriteAccess(fromWallet.getId(), user.getId());
        requireWriteAccess(toWallet.getId(), user.getId());

        Transaction tx = Transaction.builder()
                .type(TransactionType.TRANSFER)
                .status(TransactionStatus.POSTED)
                .amount(request.getAmount())
                .currency(fromWallet.getCurrency())
                .transactionDate(request.getTransactionDate())
                .category(request.getCategory())
                .description(request.getDescription())
                .fromWallet(fromWallet)
                .toWallet(toWallet)
                .createdBy(user)
                .build();

        tx = transactionRepository.save(tx);

        // Apply balance
        applyBalanceImpact(tx, false);

        return toResponse(tx);
    }

    private void applyBalanceImpact(Transaction tx, boolean reverse) {
        BigDecimal amount = tx.getAmount();

        if (tx.getType() == TransactionType.EXPENSE) {
            Wallet w = walletRepository.findById(tx.getWallet().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
            BigDecimal current = w.getCurrentBalance() != null ? w.getCurrentBalance() : BigDecimal.ZERO;
            w.setCurrentBalance(reverse ? current.add(amount) : current.subtract(amount));
            walletRepository.save(w);
            return;
        }

        if (tx.getType() == TransactionType.INCOME) {
            Wallet w = walletRepository.findById(tx.getWallet().getId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Wallet not found"));
            BigDecimal current = w.getCurrentBalance() != null ? w.getCurrentBalance() : BigDecimal.ZERO;
            w.setCurrentBalance(reverse ? current.subtract(amount) : current.add(amount));
            walletRepository.save(w);
            return;
        }

        // TRANSFER
        Wallet from = walletRepository.findById(tx.getFromWallet().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "fromWallet not found"));
        Wallet to = walletRepository.findById(tx.getToWallet().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "toWallet not found"));

        BigDecimal fromCurrent = from.getCurrentBalance() != null ? from.getCurrentBalance() : BigDecimal.ZERO;
        BigDecimal toCurrent = to.getCurrentBalance() != null ? to.getCurrentBalance() : BigDecimal.ZERO;

        from.setCurrentBalance(reverse ? fromCurrent.add(amount) : fromCurrent.subtract(amount));
        to.setCurrentBalance(reverse ? toCurrent.subtract(amount) : toCurrent.add(amount));

        walletRepository.save(from);
        walletRepository.save(to);
    }

    private void requireWriteAccess(UUID walletId, UUID userId) {
        WalletMember member = walletMemberRepository.findByWalletIdAndUserId(walletId, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Access denied"));

        if (member.getRole() == WalletRole.READER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Insufficient permissions");
        }
    }

    private void validateAmount(BigDecimal amount) {
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "amount must be > 0");
        }
    }

    private TransactionType parseType(String type) {
        if (type == null || type.isBlank()) return null;
        try {
            return TransactionType.valueOf(type.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid type");
        }
    }

    private TransactionStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return TransactionStatus.POSTED;
        try {
            return TransactionStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid status");
        }
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .type(t.getType())
                .status(t.getStatus())
                .amount(t.getAmount())
                .currency(t.getCurrency())
                .transactionDate(t.getTransactionDate())
                .category(t.getCategory())
                .description(t.getDescription())
                .merchant(t.getMerchant())
                .isItemized(t.isItemized())
                .itemCount(t.getItems() != null ? t.getItems().size() : 0)
                .walletId(t.getWallet() != null ? t.getWallet().getId() : null)
                .fromWalletId(t.getFromWallet() != null ? t.getFromWallet().getId() : null)
                .toWalletId(t.getToWallet() != null ? t.getToWallet().getId() : null)
                .createdByUserId(t.getCreatedBy() != null ? t.getCreatedBy().getId() : null)
                .build();
    }
}
