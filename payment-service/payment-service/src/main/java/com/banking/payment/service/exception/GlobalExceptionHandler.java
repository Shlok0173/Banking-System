package com.banking.payment.service.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ApiResponse> handlePaymentNotFoundException(PaymentNotFoundException ex) {
        ApiResponse apiResponse = new ApiResponse(false, ex.getMessage(), "PAYMENT_NOT_FOUND", null);
        return ResponseEntity.status(404).body(apiResponse);
    }
}
