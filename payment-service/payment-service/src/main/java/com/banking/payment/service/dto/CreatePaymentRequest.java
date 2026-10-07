package com.banking.payment.service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Getter
@Setter
public class CreatePaymentRequest {

    @NotBlank(message = "Account number is required")
    private String accountNumber;
    @NotBlank(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    private String description;
}
