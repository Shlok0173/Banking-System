package com.banking.transaction.service.service;

import com.banking.transaction.service.entity.Transaction;
import com.banking.transaction.service.entity.TransactionStatus;
import com.banking.transaction.service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Payload;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@RequiredArgsConstructor
public class TransactionEventConsumer {

    private  final TransactionRepository transactionRepository;
    private final TransactionService transactionService;
    private  final RedisTemplate redisTemplate;

    private static final long OTP_EXPIRY_MINUTES=5;

    private final KafkaTemplate <String,Object> kafkaTemplate;

    private static  final String TRANSACTION_OTP_GENERATED_TOPIC="transaction.otp.generated";
    /**
     * Consume verification.required
     * Generate OTP and ask user to verify
     * @param payload
     */

    @KafkaListener(topics = "verification.required", groupId = "transaction-service-group")
    public void counsumeVerficationRequired(
            @Payload Map<String, Object> payload) {

        try {
          String transactionId = (String) payload.get("transactionId");
          String accountNumber = (String) payload.get("accountNumber");
          String reason= (String) payload.get("reason");

          log.info("Verification required - transaction:{} reason:{} account:{}", transactionId, reason, accountNumber);

         Transaction transaction= transactionRepository.findById(transactionId).orElseThrow(
                  ()-> new RuntimeException("Transaction not found"));

          if (transaction.getStatus() != TransactionStatus.PROCESSING){
              log.warn("Transaction {} is not in processing state, current state: {}", transactionId);
          }

          // Generate otp

            String otp = String.format("%06d",(int) (Math.random() * 900000) + 10000);

           String otpKey="verfication-otp"+transactionId;

           redisTemplate.opsForValue().set(otpKey, otp,OTP_EXPIRY_MINUTES, TimeUnit.MINUTES);

           transaction.setStatus(TransactionStatus.PENDING_VERIFICATION);
           transactionRepository.save(transaction);
           log.info("Otp Generate for transaction:{} expires in {} min", transactionId,OTP_EXPIRY_MINUTES);


           // Notify user to verify otp - this can be done via email, sms, push notification etc

            Map<String,Object> optEvent=new HashMap<>();
            optEvent.put("transactionId",transactionId);
            optEvent.put("accountNumber",accountNumber);
            optEvent.put("reason",reason);
            optEvent.put("otp",otp);
            optEvent.put("ammount",payload.get("ammount"));

            kafkaTemplate.send(TRANSACTION_OTP_GENERATED_TOPIC,transactionId,optEvent);
        } catch (Exception e) {
            log.error(
                    "Error while processing verification.required event for transactionId={}", payload.get("transactionId"),
                    e
            );
        }
    }

    @KafkaListener(topics = "fraud.check.clean")
    public void consumeFraudCheckCleanResult(
            @Payload Map<String, Object> payload
    ){
        try {
            String transactionId = (String) payload.get("transactionId");
           transactionService.processCleanResult(transactionId);
        }catch (Exception e){
            log.error("Error processing fraud check result:{}",e.getMessage());
        }
    }
}