package com.banking.payment.service.serviceimpl;

import com.banking.payment.service.dto.CreatePaymentRequest;
import com.banking.payment.service.dto.PaymentOrderResponse;
import com.banking.payment.service.entity.Payment;
import com.banking.payment.service.enums.PaymentStatus;
import com.banking.payment.service.exception.PaymentNotFoundException;
import com.banking.payment.service.repository.PaymentRepository;
import com.banking.payment.service.service.PaymentService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImplementation implements PaymentService {

    private final PaymentRepository paymentRepository;
    private static final String DEFAULT_CURRENCY = "INR";


    @Override
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest createPaymentRequest) {
        log.info("Starting payment order creation");

        if (createPaymentRequest == null) {
            throw new IllegalArgumentException("CreatePaymentRequest cannot be null");
        }
        if (createPaymentRequest.getAmount() == null ||
                createPaymentRequest.getAmount().signum() <= 0) {

            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }
        if (createPaymentRequest.getAccountNumber() == null || createPaymentRequest.getAccountNumber().isBlank()) {
            throw new IllegalArgumentException("Account number is required");
        }

        Payment payment = new Payment();
        payment.setAccountNumber(createPaymentRequest.getAccountNumber());
        payment.setAmount(createPaymentRequest.getAmount());
        payment.setDescription(createPaymentRequest.getDescription());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCurrency(DEFAULT_CURRENCY);
        payment.setPaymentId(generatePaymentId());
        Payment savedPayment = paymentRepository.save(payment);
        log.info(
                "Payment order created successfully. paymentId={}, status={}",
                savedPayment.getPaymentId(),
                savedPayment.getStatus()
        );
        return mapToResponse(savedPayment);
    }

    private String generatePaymentId() {

        String paymentId = "PAY-" + UUID.randomUUID();

        log.debug("Generated paymentId={}", paymentId);

        return paymentId;

    }


    private PaymentOrderResponse mapToResponse(Payment payment) {

        PaymentOrderResponse paymentOrderResponse = new PaymentOrderResponse();
        paymentOrderResponse.setPaymentId(payment.getPaymentId());
        paymentOrderResponse.setRazorpayOrderId(payment.getRazorpayOrderId());
        paymentOrderResponse.setAmount(payment.getAmount());
        paymentOrderResponse.setStatus(payment.getStatus().name());
        paymentOrderResponse.setRazorpayKeyId(null);
        return paymentOrderResponse;
    }

    @Override
    public Payment getPaymentByPaymentId(String paymentId) {
        log.info("Fetching payment for paymentId={}", paymentId);
    if(paymentId == null || paymentId.isBlank()) {
        log.warn("Payment ID is null or blank");
        throw new IllegalArgumentException("Payment ID cannot be null or blank");
    }
        Optional<Payment> payment = paymentRepository.findByPaymentId(paymentId);

        if (payment.isEmpty()) {
            log.warn("Payment not found for paymentId={}", paymentId);
            throw new PaymentNotFoundException(
                    "Payment not found for paymentId: " + paymentId
            );
        }

        log.info("Payment found for paymentId={}", paymentId);
        return payment.get();
    }
}





