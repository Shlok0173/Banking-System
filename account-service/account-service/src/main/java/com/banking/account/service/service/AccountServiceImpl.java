package com.banking.account.service.service;

import com.banking.account.service.dto.AccountResponse;
import com.banking.account.service.dto.CreateAccountRequest;
import com.banking.account.service.entity.Account;
import com.banking.account.service.entity.AccountStatus;
import com.banking.account.service.entity.AccountType;
import com.banking.account.service.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class AccountServiceImpl  implements AccountService{

    private final AccountRepository accountRepository;

    private static SecureRandom secureRandom =new SecureRandom();

    /**
     * Creates a new account
     * @param request
     * @return response
     */
    @Override
    public AccountResponse createAccount(CreateAccountRequest request) {
        log.info("Creating account for: {}", request.getEmail());

        if(accountRepository.existsByEmail(request.getEmail())){
            log.info("Account already exists for: {}", request.getEmail());
            throw new RuntimeException("Account already exists for: " + request.getEmail());
        }
        Account account = new Account();
        account.setAccountHolderName(request.getAccountHolderName());
        account.setEmail(request.getEmail());
        account.setPhone(request.getPhone());
        account.setAccountType(request.getAccountType());
        account.setAccountType(AccountType.SAVINGS);
        account.setStatus(AccountStatus.ACTIVE);
        account.setBalance(request.getInitialDeposit());
        account.setAccountNumber(generateAccountNumber());
        account.setDailyTransactionLimit(
                request.getAccountType()== AccountType.SAVINGS ?
                        BigDecimal.valueOf(100000)
                        : BigDecimal.valueOf(500000));

        Account savedAccount = accountRepository.save(account);
        log.info("Saved account for: {}", savedAccount.getAccountNumber());
        return mapToResponse(savedAccount);
    }

    /**
     *
     * @return
     */
    private String generateAccountNumber() {
        String accountNumber;
        do{
            long number = secureRandom.nextLong(1_000_000_000_000L);
         accountNumber=String.format("%012d",number);
        }while(accountRepository.existsByAccountNumber(accountNumber));
        return accountNumber;
    }


    /**
     * Get account by account number
     * @param accountNumber
     * @return
     */
    @Override
    public AccountResponse getAccount(String accountNumber) {
         Account account= accountRepository.findByAccountNumber(accountNumber).
                 orElseThrow(()->new RuntimeException("Account not found for number: " + accountNumber));
        return mapToResponse(account);
    }
    @Override
    public BigDecimal getBalance(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(()->new RuntimeException("Account not found for number: " + accountNumber));
        return account.getBalance();
    }

    /**
     * Block account-called by Fraud detection Service via kafka
     * @param accountNumber
     */
    @Override
    public void blockedAccount(String accountNumber) {
        log.info("Request accepted to block account: {}", accountNumber);

       Account account = accountRepository.findByAccountNumber(accountNumber)
               .orElseThrow(()->{
                   log.error("Account not found: {}", accountNumber);
                   return new RuntimeException("Account not found for number: " + accountNumber);
               });
        if (account.getStatus() == AccountStatus.BLOCKED) {
            log.warn("Account {} is already blocked.", accountNumber);
            return;
        }

       account.setStatus(AccountStatus.BLOCKED);
       accountRepository.save(account);

    }

    /**
     * Credit Balance
     * Called by Transaction Service
     * @param accountNumber
     * @param amount
     */
    @Override
    public void creditBalance(String accountNumber, BigDecimal amount) {
        log.info("Credit request received. Account: {}, Amount: {}", accountNumber, amount);

        if(amount==null || amount.compareTo(BigDecimal.ZERO)<=0){
            log.error("Invalid  amount: {}", amount);
            throw new RuntimeException("Amount must be greater than zero");
        }
        Account account = accountRepository.findByAccountNumber(accountNumber).
                orElseThrow(() -> {
            log.error("Account not found: {}", accountNumber);
            return new RuntimeException("Account not found for number: " + accountNumber);

        });
        if (account.getStatus() == AccountStatus.BLOCKED) {
            throw new IllegalStateException("Blocked account cannot be credited.");
        }
        account.setBalance(account.getBalance().add(amount));
        accountRepository.save(account);
        log.info("Amount {} credited successfully to account {}", amount, accountNumber);
    }

    /**
     * Deduct balance from sender account
     * Called by Transaction Service
     * @param accountNumber
     * @param amount
     */
    @Override
    public void deductBalance(String accountNumber, BigDecimal amount) {
     log.info("Deducting balance {} from account {}", amount, accountNumber);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be greater than zero.");
        }

        Account account = accountRepository.findByAccountNumber(accountNumber).
                orElseThrow(() -> {
                    log.error("Account not found: {}", accountNumber);
                    return new RuntimeException("Account not found for number: " + accountNumber);

                });

        if(account.getStatus() != AccountStatus.ACTIVE){
            throw  new RuntimeException("Account is not active for number: " + accountNumber);
        }

        if( account.getStatus() == AccountStatus.ACTIVE && account.getBalance().compareTo(amount) < 0){
            log.error("Insufficient balance. Account: {}, Balance: {}, Requested: {}",
                    accountNumber,
                    account.getBalance(),
                    amount);
            throw new RuntimeException("Insufficient balance in account: " + accountNumber);
        }

        account.setBalance(account.getBalance().subtract(amount));
        accountRepository.save(account);
        log.info("Amount {} deducted successfully. Account: {}, Remaining Balance: {}",
                amount,
                accountNumber,
                account.getBalance());
    }

    /**
     *
     * convert account to dto
     * @return response
     */
    private AccountResponse mapToResponse(Account savedAccount) {
        return AccountResponse.builder()
                .id(savedAccount.getId())
                .accountNumber(savedAccount.getAccountNumber())
                .accountHolderName(savedAccount.getAccountHolderName())
                .email(savedAccount.getEmail())
                .phone(savedAccount.getPhone())
                .accountType(savedAccount.getAccountType())
                .status(savedAccount.getStatus())
                .balance(savedAccount.getBalance())
                .dailyTransactionLimit(savedAccount.getDailyTransactionLimit())
                .createdAt(savedAccount.getCreatedAt())
                .build();
    }
}
