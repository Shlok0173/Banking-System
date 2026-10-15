package com.banking.payment.service.service;

import com.banking.payment.service.dto.CreatePaymentRequest;
import com.banking.payment.service.dto.PaymentOrderResponse;
import com.banking.payment.service.entity.Payment;
import com.razorpay.RazorpayException;

import java.util.Map;
import java.util.Optional;

public interface PaymentService {

    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest createPaymentRequest) throws RazorpayException;

    public Payment getPaymentByPaymentId(String paymentId) throws RazorpayException;

    void handelwebhook(Map<String, Object> payload);
}
