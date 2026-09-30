package com.banking.payment.service.service;

import com.banking.payment.service.dto.CreatePaymentRequest;
import com.banking.payment.service.dto.PaymentOrderResponse;
import com.banking.payment.service.entity.Payment;

import java.util.Optional;

public interface PaymentService {

    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest createPaymentRequest);

    public Payment getPaymentByPaymentId(String paymentId);
}
