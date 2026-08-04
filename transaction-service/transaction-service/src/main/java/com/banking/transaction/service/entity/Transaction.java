package com.banking.transaction.service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.security.PrivateKey;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@Setter
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String  id;

    @Column(nullable = false)
    private String  senderAccountNumber;

    @Column(nullable = false)
    private String  receiverAccountNumber;

    @Column(nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;
    private String description;
    private String failureReason;
    private String referenceNumber;
    @CreationTimestamp
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    private LocalDateTime completedAt;

}
