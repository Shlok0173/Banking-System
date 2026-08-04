package com.banking.account.service.dto;

import com.banking.account.service.entity.AccountStatus;
import com.banking.account.service.entity.AccountType;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Setter
@Getter
@Builder
public class AccountResponse {
    private String id;
    private String accountNumber;
    private String accountHolderName;
    private String email;
    private String phone;
    private AccountType accountType;
    private AccountStatus status;
    private BigDecimal balance;
    private  BigDecimal dailyTransactionLimit;
    private LocalDateTime createdAt;

}
