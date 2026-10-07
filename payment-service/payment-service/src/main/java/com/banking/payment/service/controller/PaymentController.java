package com.banking.payment.service.controller;

import com.banking.payment.service.dto.CreatePaymentRequest;
import com.banking.payment.service.dto.PaymentOrderResponse;
import com.banking.payment.service.entity.Payment;
import com.banking.payment.service.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController("/api/v1")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {


    private final PaymentService paymentService;

    @PostMapping("/payments")
    public ResponseEntity<PaymentOrderResponse> createPayment(@Valid @RequestBody CreatePaymentRequest request) {
        log.info("Received request to create payment");
        PaymentOrderResponse servicePaymentOrder = paymentService.createPaymentOrder(request);
        log.info(
                "Payment created successfully. paymentId={}",
                servicePaymentOrder.getPaymentId()
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(servicePaymentOrder);
    }

    @GetMapping("/payments/{paymentId}")
    public ResponseEntity<Payment> getPaymentById(@PathVariable String paymentId) {
        log.info("Received request to get payment by id: {}", paymentId);
        Payment servicePaymentOrder = paymentService.getPaymentByPaymentId(paymentId);
        log.info(
                "Payment retrieved successfully. paymentId={}",
                servicePaymentOrder.getPaymentId()
        );

        return ResponseEntity.status(HttpStatus.OK).body(servicePaymentOrder);
    }
}
