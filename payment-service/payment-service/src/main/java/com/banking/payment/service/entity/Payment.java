package com.banking.payment.service.entity;

import com.banking.payment.service.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "payments",
        indexes = {
                @Index(
                        name = "idx_payment_id",
                        columnList = "payment_id"
                ),
                @Index(
                        name = "idx_account_number",
                        columnList = "account_number"
                ),
                @Index(
                        name = "idx_razorpay_order_id",
                        columnList = "razorpay_order_id"
                )
        }
)
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Setter
@Getter
public class Payment {


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @Column(
            name = "payment_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 50
    )
    private String paymentId;

    /**
     * Razorpay order ID.
     * It will be populated when Razorpay order is created.
     */
    @Column(
            name = "razorpay_order_id",
            unique = true
    )
    private String razorpayOrderId;

    /**
     * Razorpay transaction/payment ID.
     */
    @Column(name = "razorpay_transaction_id")
    private String razorpayTransactionId;

    /**
     * Customer bank account number.
     */
    @Column(
            name = "account_number",
            nullable = false,
            length = 30
    )
    private String accountNumber;

    /**
     * Payment amount.
     */
    @Column(
            nullable = false,
            precision = 15,
            scale = 2
    )
    private BigDecimal amount;

    /**
     * Payment currency.
     * Default: INR
     */
    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    /**
     * Payment status.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private PaymentStatus status;

    /**
     * Customer payment description.
     */
    @Column(length = 255)
    private String description;


    @Column(
            name = "failure_reason",
            length = 500
    )
    private String failureReason;


    @CreationTimestamp
    @Column(
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Record last update time.
     */
    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

}
