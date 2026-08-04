package com.banking.account.service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Map;

@Service
@Slf4j

public class AccountEventConsumer {


    private AccountService accountService;
    /**
     * Consumer transaction.completed event from kafka
     * Credits Reciver Account
     * @param payload
     */
    @KafkaListener(topics = "transaction.completed")
    public  void consumeTransactionCompleted(
            @Payload Map<String, Object> payload){
        try {
            String receiverAccountNumber = (String) payload.get("receiverAccountNumber");
            BigDecimal amount=new BigDecimal(payload.get("amount").toString());

            log.info("Crediting account: {} amount: {}", receiverAccountNumber,amount);
            if (receiverAccountNumber == null || receiverAccountNumber.isBlank()) {
                throw new IllegalArgumentException("Receiver account number is missing.");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }

            accountService.creditBalance(receiverAccountNumber,amount);

        }catch (Exception ex){
            log.error(
                    "Failed to process transaction.completed event. Payload: {}",
                    payload,
                    ex);
        }

    }


    /**
     * Consumer fraud.detected event from kafka
     * @param payload
     */
    @KafkaListener(topics = "fraud.detected")
    public void consumeFraudDetected(
            @Payload Map<String, Object> payload){
        try {
            String receiverAccountNumber = (String) payload.get("receiverAccountNumber");
            BigDecimal amount=new BigDecimal(payload.get("amount").toString());
            log.info("Received fraud.detected event. Receiver Account: {}, Amount: {}",
                    receiverAccountNumber,
                    amount);
            if (receiverAccountNumber == null || receiverAccountNumber.isBlank()) {
                throw new IllegalArgumentException("Receiver account number is missing.");
            }

            if (amount.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalArgumentException("Amount must be greater than zero.");
            }

            accountService.deductBalance(receiverAccountNumber,amount);
            log.info("Successfully deducted {} from account {}",
                    amount,
                    receiverAccountNumber);

        } catch (Exception e) {
            log.error(
                    "Failed to process fraud.detected event. Payload: {}",
                    payload,
                    e);
        }
    }
}
