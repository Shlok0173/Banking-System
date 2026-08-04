package com.banking.account.service.controller;

import com.banking.account.service.dto.AccountResponse;
import com.banking.account.service.dto.CreateAccountRequest;
import com.banking.account.service.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.micrometer.observation.autoconfigure.ObservationProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountService accountService;


    /**
     * Create new account
     * @param request
     * @return
     */
    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(@Valid @RequestBody CreateAccountRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(accountService.createAccount(request));
    }


    /**
     *
     * @param accountNumber
     * @return
     */

    @GetMapping("/{accountNumber}")
    public ResponseEntity<AccountResponse> getAccount(@PathVariable String accountNumber) {
        return ResponseEntity.ok(accountService.getAccount(accountNumber));
    }

    /**
     *
     * @param accountNumber
     * @return
     */

    @GetMapping("/{accountNumber}/balance")
    public ResponseEntity<BigDecimal> getBalance(@PathVariable String accountNumber){
        return ResponseEntity.ok(accountService.getBalance(accountNumber));
    }

    /**
     *
     * @param accountNumber
     * @return
     */
    @PutMapping("/{accountNumber}/blocked")
    public ResponseEntity<String> blockedAccount(@PathVariable String accountNumber){
        accountService.blockedAccount(accountNumber);
        return ResponseEntity.ok("Account has been blocked");

    }

    /**
     * SAGA STEP 1- Deduct Balance
     * Called by Transaction Service when transfer is initiated
     *
     */

    @PutMapping("{accountNumber}/deduct")
    public ResponseEntity<String > deductBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount){
        // Call the service to deduct the balance
        accountService.deductBalance(accountNumber, amount);
        return ResponseEntity.ok("Balance deducted successfully");
    }

    /**
     * SAGA STEP-4 Compenstating  transaction endpoint
     *CALLED BY TRANSACTION SERVICE in TWO SCENARIOS
     * 1. Fraud detected ->refund sender
     * 2.Transaction completed-> Credit receiver
     */

    @PutMapping("{accountNumber}/credit")
    public ResponseEntity<String>creditBalance(
            @PathVariable String accountNumber,
            @RequestParam BigDecimal amount){
        accountService.creditBalance(accountNumber,amount);
        return ResponseEntity.ok("Balance credited successfully");
    }
}
