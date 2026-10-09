package com.banking.payment.service.serviceimpl;

import com.banking.payment.service.dto.CreatePaymentRequest;
import com.banking.payment.service.dto.PaymentOrderResponse;
import com.banking.payment.service.entity.Payment;
import com.banking.payment.service.enums.PaymentStatus;
import com.banking.payment.service.exception.PaymentNotFoundException;
import com.banking.payment.service.repository.PaymentRepository;
import com.banking.payment.service.service.PaymentService;
import com.razorpay.Order;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PaymentServiceImplementation implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    private static final String DEFAULT_CURRENCY = "INR";
    private static final String PAYMENT_COMPLETED_TOPIC="payment.completed";
    private static final String PAYMENT_CANCELED_TOPIC="payment.cancelled";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Create  Razorpay payment order
     *
     * FLOW
     * 1.Create order in razorpay
     * 2.save payment recorded in DB
     * Retuen order details to frontend
     * Frontent show Razorpay Checkout
     * User pays
     * Razorpay call webhook
     *
     * @param createPaymentRequest
     * @return
     */
    @Override
    public PaymentOrderResponse createPaymentOrder(CreatePaymentRequest createPaymentRequest) throws RazorpayException {
        log.info("Creating payment order for account:{}",createPaymentRequest.getAccountNumber(),
                createPaymentRequest.getAmount());

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

        RazorpayClient razorpayClient =
                new RazorpayClient(keyId, keySecret);

        BigDecimal amount = createPaymentRequest.getAmount();

        if (amount == null || amount.signum() <= 0) {
            throw new IllegalArgumentException(
                    "Amount must be greater than zero"
            );
        }

// Convert INR to paise without losing decimal precision
        long convertedAmount;

        try {
            convertedAmount = amount
                    .setScale(2, RoundingMode.UNNECESSARY)
                    .movePointRight(2)
                    .longValueExact();
        } catch (ArithmeticException ex) {
            throw new IllegalArgumentException(
                    "Amount must have maximum two decimal places", ex
            );
        }

        JSONObject orderRequest = new JSONObject();
        orderRequest.put("amount", convertedAmount);
        orderRequest.put("currency", "INR");
        orderRequest.put(
                "receipt",
                "rcpt-" + UUID.randomUUID()
        );

        Order razorpayOrder =
                razorpayClient.orders.create(orderRequest);

        String razorpayOrderId = razorpayOrder.get("id");

        log.info(
                "Razorpay order created. accountNumber={}, orderId={}",
                createPaymentRequest.getAccountNumber(),
                razorpayOrderId
        );

        //saved order in side db
        Payment payment = new Payment();
        payment.setPaymentId(generatePaymentId());
        payment.setRazorpayOrderId(razorpayOrderId.toString());
        payment.setAccountNumber(createPaymentRequest.getAccountNumber().trim());
        payment.setAmount(createPaymentRequest.getAmount());
        payment.setStatus(PaymentStatus.CREATED);
        payment.setCurrency(DEFAULT_CURRENCY);
        payment.setDescription(createPaymentRequest.getDescription());
        Payment savedPayment = paymentRepository.save(payment);
        log.info(
                "Payment order created successfully. paymentId={}, status={}",
                savedPayment.getPaymentId(),
                savedPayment.getStatus()
        );
        return new PaymentOrderResponse(
          savedPayment.getPaymentId(),
          razorpayOrder.get("id").toString(),
          createPaymentRequest.getAmount(),
          "INR",
             "CREATED",
             keyId
        );
    }

    private String generatePaymentId() {

        String paymentId = "PAY-" + UUID.randomUUID();

        log.debug("Generated paymentId={}", paymentId);

        return paymentId;

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

    @Override
    public void handelwebhook(Map<String, Object> payload) {


        log.info("Received Razorpay webhook event={}",
                payload.get("event"));
        String event=(String) payload.get("event");
        if("payment.captured".equals(event)) {
            handelPayementSuccess(payload);
        }else if("payment.failed".equals(event)) {
            handelPaymentFailure(payload);
        }
    }




    private void handelPayementSuccess(Map<String, Object> payload) {
        try {
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderid = (String) paymentData.get("order_id");
            String paymentId = (String) paymentData.get("id");

            Payment payment = paymentRepository.findByRazorpayOrderId(orderid).orElseThrow(
                    () -> new RuntimeException(
                            "Payment not found for orderId: " + orderid
                    ));
           // payment.setRazorpayOrderId(paymentId);
            payment.setRazorpayTransactionId(paymentId);
            payment.setStatus(PaymentStatus.COMPLETED);
            paymentRepository.save(payment);


            // publish payment complete event

            Map<String,Object> event=new HashMap<>();
            event.put("paymentId", payment.getPaymentId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayaPaymentId", paymentId);

            kafkaTemplate.send(PAYMENT_COMPLETED_TOPIC,payment.getPaymentId(), event);
            log.info("Payment completed successfully{}",payment.getPaymentId());
        } catch(Exception ex) {
            log.error("Payment failed",ex);
        }
    }



    private void handelPaymentFailure(Map<String, Object> payload) {

        try {
            Map<String, Object> paymentData = extractPaymentData(payload);
            String orderId=(String) paymentData.get("order_id");
            String razorpayPaymentId=(String) paymentData.get("id");
            String failureReason =
                    (String) paymentData.get("error_description");

          Payment  payment =paymentRepository.findByRazorpayOrderId(orderId).
                    orElseThrow(()-> new RuntimeException("Payment not found for orderId: " + orderId));
                  // payment.setRazorpayOrderId(razorpayPaymentId);
                   payment.setRazorpayTransactionId(razorpayPaymentId);
                   payment.setStatus(PaymentStatus.FAILED);
                   payment.setFailureReason(failureReason);
                   paymentRepository.save(payment);

                   // publish payment failed event

            Map<String,Object> event=new HashMap<>();
            event.put("paymentId", payment.getPaymentId());
            event.put("accountNumber", payment.getAccountNumber());
            event.put("amount", payment.getAmount());
            event.put("razorpayaPaymentId", razorpayPaymentId);
            event.put("paymentFailed", payment.getStatus());
            event.put("failureReason", failureReason);

            kafkaTemplate.send(PAYMENT_CANCELED_TOPIC,payment.getPaymentId(), event);
            log.info("Payment failed. paymentId={}",
                    payment.getPaymentId());
        }catch(Exception ex) {
            log.error("Failed to process payment failure webhook", ex);
            throw ex;
        }
    }
    private Map<String, Object> extractPaymentData(Map<String, Object> payload) {
        Map<String, Object> payloadData =
                (Map<String, Object>) payload.get("payload");

        if (payloadData == null) {
            throw new IllegalArgumentException(
                    "Missing payload data");
        }

        Map<String, Object> paymentData =
                (Map<String, Object>) payloadData.get("payment");

        if (paymentData == null) {
            throw new IllegalArgumentException(
                    "Missing payment data");
        }

        Map<String, Object> entity =
                (Map<String, Object>) paymentData.get("entity");

        if (entity == null) {
            throw new IllegalArgumentException(
                    "Missing payment entity");
        }

        return entity;
    }
}





