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

    @KafkaListener(topics = "transaction.completed")
    public void consumerTransactionCompleted(
            @Payload  Map<String, Object> payload){
        try {
              String senderAccountNumber = (String) payload.get("senderAccountNumber");
              String receiverAccountNumber = (String) payload.get("receiverAccountNumber");
              String amount = (String) payload.get("amount").toString();

            sendAlert(senderAccountNumber,"TRANSACTION COMPLETED", String.format("A transaction of %s has been completed successfully. "+
                            "Receiver Account Number: %s. "+
                            "If this was not initiated by you, please contact customer support immediately.", amount, senderAccountNumber));
            sendAlert(receiverAccountNumber,"TRANSACTION COMPLETED", String.format("A transaction of %s has been completed successfully. "+
                            "Sender Account Number: %s. "+
                            "If this was not initiated by you, please contact customer support immediately.", amount, receiverAccountNumber));
        } catch (Exception e){
            log.error("Error sending transaction completed notificaton", e.getMessage()
            );
        }
    }



    @KafkaListener(topics = "fraud.detected")
   public void consumerFraudDetected(
           @Payload  Map<String, Object> payload){
       try {
           String senderAccountNumber = (String) payload.get("senderAccountNumber");
           String reason = (String) payload.get("reason");
              sendAlert(senderAccountNumber,"FRAUD DETECTED", String.format("Suspicious activity detected on your account. "+
                      "Reason: %s. "+
                      "If this was not initiated by you, please contact customer support immediately.", reason));
       } catch (Exception e) {
           log.error("Error sending fraud detected notification", e.getMessage());
       }
    }

    @KafkaListener(topics = "transaction.refunded")
    public void consumerTransactionRefunded(
            @Payload  Map<String, Object> payload){
        try {
            String senderAccountNumber = (String) payload.get("senderAccountNumber");
            String amount = (String) payload.get("amount").toString();
            String reason=(String) payload.get("reason");
            sendAlert(senderAccountNumber,"TRANSACTION REFUNDED", String.format("A transaction of %s has been refunded successfully. "+
                            "If this was not initiated by you, please contact customer support immediately.", amount));
        } catch (Exception e){
            log.error("Error sending transaction refunded notificaton", e.getMessage()
            );
        }
    }

    @KafkaListener(topics = "payment.completed")
    public void consumePaymentCompleted(
            @Payload  Map<String, Object> payload){
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = (String) payload.get("amount").toString();
            sendAlert(accountNumber,"PAYMENT COMPLETED", String.format("A payment of %s has been completed successfully. "+
                            "If this was not initiated by you, please contact customer support immediately.", amount,payload.get("razorpayPaymentId")));
        } catch (Exception e){
            log.error("Error sending payment completed notificaton", e.getMessage()
            );
        }
    }

    @KafkaListener(topics = "payment.cancelled")
    public void consumePaymentCancelled(
            @Payload  Map<String, Object> payload){
        try {
            String accountNumber = (String) payload.get("accountNumber");
            String amount = (String) payload.get("amount").toString();
            String reason=(String) payload.get("reason");
            sendAlert(accountNumber,"PAYMENT CANCELLED", String.format("A payment of %s has been cancelled successfully. "+
                            "If this was not initiated by you, please contact customer support immediately.", amount,payload.get("razorpayPaymentId")));
        } catch (Exception e){
            log.error("Error sending payment cancelled notificaton", e.getMessage()
            );
        }
    }
    private void sendAlert(String accountNumber, String subject, String message) {
       log.info("----------------------------------------------------------");
       log.info("Account:{}",accountNumber);
       log.info("Subject:{}",subject);
       log.info("Message:{}",message);
       log.info("-----------------------------------------------------------");
    }
}
