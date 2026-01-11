package com.tigtech.persfinance.service;

import com.tigtech.persfinance.domain.User;
import com.tigtech.persfinance.web.dto.CreateTransactionRequest;
import com.tigtech.persfinance.web.dto.ListTransactionsResponse;
import com.tigtech.persfinance.web.dto.TransactionResponse;

import java.time.Instant;
import java.util.UUID;

public interface TransactionService {

    TransactionResponse createTransaction(User user, CreateTransactionRequest request);

    ListTransactionsResponse listTransactions(User user,
                                            UUID walletId,
                                            Instant from,
                                            Instant to,
                                            String type,
                                            String q,
                                            String status);

    TransactionResponse softDeleteTransaction(User user, UUID transactionId);
}
