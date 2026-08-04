package com.banking.fraud_detection_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class FraudDetectionEventConsumer {

    private  final FraudDetectionService fraudDetectionService;

    /**
     * Listens to transaction.initiated topic
     * Every transaction goes through fraud detection before it is completed
     * @param payload
     */

    @KafkaListener(topics = "transaction.initiated",groupId="fraud-detection-group" )
    public void consumeTransactionInitiated(
            @Payload Map<String ,Object> payload){
        log.info("Consuming transaction initiated event with payload: {}", payload.get("transactionId"));
            try {
                fraudDetectionService.checkTransaction(payload);
            } catch (Exception e) {
                log.error("Error occurred while consuming transaction initiated event", e);
            }
    }
}
