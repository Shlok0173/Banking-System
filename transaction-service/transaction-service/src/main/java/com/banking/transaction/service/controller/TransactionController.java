package com.banking.transaction.service.controller;

import com.banking.transaction.service.dto.TransferRequest;
import com.banking.transaction.service.dto.TransactionResponse;
import com.banking.transaction.service.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/v1/transactions")
@RequiredArgsConstructor
@Slf4j
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    ResponseEntity<TransactionResponse> transfer(TransferRequest transfer) {
        return  ResponseEntity.status(HttpStatus.CREATED).body(transactionService.transfer(transfer));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<TransactionResponse> getTransaction(
            @PathVariable String transaction_id
    ){
        return ResponseEntity.ok(transactionService.getTransaction(transaction_id));
    }

    @GetMapping("/account/{accountNumber}")
    public ResponseEntity<List<TransactionResponse>> getTransactionHistory(
            @PathVariable String accountNumber
    ){
        return ResponseEntity.ok(transactionService.getTransactionHistory(accountNumber));
    }


    @PostMapping("/{transactionId}/verify")
    public ResponseEntity<TransactionResponse> verifyOtp(
            @PathVariable String transaction_id,
            @PathVariable String otp
    ){
        return ResponseEntity.ok(transactionService.verifyOtp(transaction_id,otp));
    }
}
