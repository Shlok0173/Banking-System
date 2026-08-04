package com.banking.account.service.service;

import com.banking.account.service.dto.AccountResponse;
import com.banking.account.service.dto.CreateAccountRequest;
import jakarta.validation.Valid;

import java.math.BigDecimal;

public interface AccountService {
    BigDecimal getBalance( String accountNumber);

    AccountResponse createAccount(@Valid CreateAccountRequest request);

    AccountResponse getAccount(String accountNumber);

    void blockedAccount(String accountNumber);

    void creditBalance(String accountNumber, BigDecimal amount);

    void deductBalance(String accountNumber, BigDecimal amount);
}
