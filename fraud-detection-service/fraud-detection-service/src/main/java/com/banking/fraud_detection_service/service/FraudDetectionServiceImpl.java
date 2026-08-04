package com.banking.fraud_detection_service.service;

import com.banking.fraud_detection_service.client.AccountServiceClient;
import com.banking.fraud_detection_service.model.FraudCheckResult;
import io.swagger.v3.oas.annotations.servers.Server;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudDetectionServiceImpl implements FraudDetectionService {

    private final AccountServiceClient accountServiceClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RedisTemplate<String, String> redisTemplate;


    @Value("&{fraud.max-transactions-per-minute}")
    private final int maxTransactionPerMinute;

    @Value("${fraud.suspicious-amount-multiplier}")
    private double suspiciousAmountMultiplier;

    @Value("${fraud.max-balance-percentage")
    private double maxBalancePercentage;

    private static final String AVG_AMOUNT_KEY = "fraud:avg_amount:";

    private static final String VERIFICATION_REQUIRED_TOPIC = "verification.required";
    private static final String FRAUD_CHECK_CLEAN_RESULT_TOPIC = "fraud.check.clean";


    @Override
    public void checkTransaction(Map<String, Object> payload) {
        String transactionId = (String) payload.get("transactionId");
        String accountNumber = (String) payload.get("accountNumber");
        BigDecimal ammount = new BigDecimal((String) payload.get("amount").toString());

        BigDecimal senderBalance = accountServiceClient.getBalance(accountNumber);

        log.info("Checking transaction: {} account: {} ammount: {}  balance: {}",
                transactionId, accountNumber, ammount, senderBalance);

        FraudCheckResult result = performFraudChecks(accountNumber, ammount, senderBalance);
        if (result.isFraud()) {
            log.info("Suspicious activity detected - account:{}" +
                    "reason:{} - requestion OTP verification", accountNumber, result.getReason());


            Map<String, Object> verficationEvent = new HashMap<>();
            verficationEvent.put("transactionId", transactionId);
            verficationEvent.put("accountNumber", accountNumber);
            verficationEvent.put("amount", ammount);
            verficationEvent.put("reason", result.getReason());

            kafkaTemplate.send(VERIFICATION_REQUIRED_TOPIC, transactionId, verficationEvent);

        } else {
            log.info("Transaction {} passed fraud checks.", transactionId);

            Map<String, Object> approvalEvent = new HashMap<>();
            approvalEvent.put("transactionId", transactionId);
            approvalEvent.put("isFraud", false);
            approvalEvent.put("reason", null);

            kafkaTemplate.send(FRAUD_CHECK_CLEAN_RESULT_TOPIC, transactionId, approvalEvent);
        }
    }

    private FraudCheckResult performFraudChecks(
            String accountNumber,
            BigDecimal amount,
            BigDecimal senderBalance) {


        // Pattern 1: Velocity check

        if (isVelocityExceeded(accountNumber)) {
            return new FraudCheckResult(
                    true,
                    "Too many transaction in 60 seconds" +
                            "Velocity limit exceeded");
        }

        //Pattern 2: Ammount check

        if (isAmountSuspicious(accountNumber, amount)) {
            return new FraudCheckResult(
                    true, "Unusual transaction amount" +
                    "- exceeds #x your average transaction amount"

            );
        }

        // Pattern 3: Balance Check

        if (amount.compareTo(BigDecimal.ZERO) > 0 && isBalanceCheckFailed(senderBalance, amount)) {
            return new FraudCheckResult(
                    true, "Transaction exceed 90% of account balance - possible fraud");
        }
        return new FraudCheckResult(false, null);
    }



    private boolean isVelocityExceeded(String accountNumber) {
        String key = "fraud:velocity" + accountNumber;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, 60, java.util.concurrent.TimeUnit.SECONDS);
        }
     log.info("Velocity check - account:{} count:{}/{}", accountNumber, count, maxTransactionPerMinute);
        return count != null && count > maxTransactionPerMinute;
    }



    private boolean isAmountSuspicious(String accountNumber, BigDecimal amount) {

        String avgKey=AVG_AMOUNT_KEY + accountNumber;
        String avgStr=redisTemplate.opsForValue().get(avgKey);

        if(avgStr==null){
            redisTemplate.opsForValue().set(avgKey, amount.toString());
            return false;
        }
        BigDecimal avg=new BigDecimal(avgStr);
        BigDecimal threshold=avg.multiply(
                BigDecimal.valueOf(suspiciousAmountMultiplier));

        // update running average

        BigDecimal newAvrg=avg.add(amount).
                divide(BigDecimal.valueOf(2),2, RoundingMode.HALF_UP);
        redisTemplate.opsForValue().set(avgKey,newAvrg.toString());
        log.info("Amount check -ammount: {} threshold: {}  suspicious:{}" ,
                amount, threshold,amount.compareTo(threshold)>0);
        return  amount.compareTo(threshold)>0;
    }



    private boolean isBalanceCheckFailed(BigDecimal senderBalance, BigDecimal amount) {
        BigDecimal maxAllow= senderBalance.multiply(BigDecimal.valueOf(maxBalancePercentage));
        log.info("Balance check - amount: {} maxAllowed: {} suscpious: {}",
                amount,maxAllow,amount.compareTo(maxAllow)>0);

        return amount.compareTo(maxAllow)>0;
    }
}
