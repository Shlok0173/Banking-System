package com.banking.transaction.service.dto;

import com.banking.transaction.service.entity.TransactionStatus;
import com.banking.transaction.service.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private String  id;
    private String  senderAccountNumber;
    private String  receiverAccountNumber;
    private BigDecimal amount;
    private TransactionType type;
    private TransactionStatus status;
    private String description;
    private String failureReason;
    private String referenceNumber;
    private LocalDateTime createdDate;
    private LocalDateTime completedAt;
}
