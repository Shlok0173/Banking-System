package com.banking.transaction.service.service;

import com.banking.transaction.service.dto.TransferRequest;
import com.banking.transaction.service.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {

    TransactionResponse transfer(TransferRequest transfer);

    TransactionResponse getTransaction(String transactionId);

   List<TransactionResponse> getTransactionHistory(String accountNumber);

    TransactionResponse verifyOtp(String transactionId, String otp);

    void processCleanResult(String transactionId);
}
