package com.banking.notification.service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@Slf4j
public class NotificationService {


   @KafkaListener(topics = "transaction.otp.generated")
    public  void consumerOtpGenerated(
            @Payload  Map<String, Object> payload){
         try {
             String transactionId = (String) payload.get("transactionId");
             String accountNumber= (String) payload.get("accountNumber");
             String amount = (String) payload.get("amount");
             String reason = (String) payload.get("reason");
             String otp = (String) payload.get("otp");

             sendAlert(accountNumber,"TRANSACTION VERFICIATION REQUIRED",
                     String.format("Suspicious activity detected on your account. "+
                             "Reason: %s "+
                             "A transaction of %s is pending verfication. "+
                             "Your OTP is: %s. Valid for 5 minutes. "+
                             "If this was not initiated by you, please contact customer support immediately."));
         } catch (Exception e){
             log.error("Error sending OTP notificaton", e.getMessage()
             );
         }
    }

    private void sendAlert(String accountNumber, String subject, String message) {
    }
}
