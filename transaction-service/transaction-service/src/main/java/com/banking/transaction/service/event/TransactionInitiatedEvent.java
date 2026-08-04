package com.banking.transaction.service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionInitiatedEvent {
    private String transactionId;
    private String senderAccountNumber;
    private String receiverAccountNumber;
    private BigDecimal ammount;
    private String description;
}
