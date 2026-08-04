package com.banking.transaction.service.service;

import com.banking.transaction.service.client.AccountServiceClient;
import com.banking.transaction.service.dto.TransactionResponse;
import com.banking.transaction.service.dto.TransferRequest;
import com.banking.transaction.service.entity.Transaction;
import com.banking.transaction.service.entity.TransactionStatus;
import com.banking.transaction.service.entity.TransactionType;
import com.banking.transaction.service.event.TransactionInitiatedEvent;
import com.banking.transaction.service.repository.TransactionRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionServiceImpl implements TransactionService{

    private  final AccountServiceClient accountServiceClient;

    private final TransactionRepository transactionRepository;

    private final KafkaTemplate<String,Object> kafkaTemplate;

    private static final String TRANSACTION_INITIARED_TOPIC="transaction.initialed";
    private static  final String TRANSACTION_COMPLETED_TOPIC="transaction.completed";
    private static  final String TRANSACTION_REFUNDED_TOPIC="transaction.refunded";

    /**
     * SAGA STEP -1:Initiate request
     * Deducts from sender via feign
     * Saves transaction as PROCESSING
     * Publish event to kafka for fraud check
     * Return
     *
     * @param request
     * @return
     */
    @Override
    public TransactionResponse transfer(TransferRequest request) {
        log.info(
                "Transfer started. sender={}, receiver={}, amount={}",
                request.getSenderAccountNumber(),
                request.getReceiverAccountNumber(),
                request.getAmount()
        );
        accountServiceClient.deductBalance(
             request.getSenderAccountNumber(),
             request.getAmount());

        Transaction transaction = new Transaction();
        transaction.setSenderAccountNumber(request.getSenderAccountNumber());
        transaction.setReceiverAccountNumber(request.getReceiverAccountNumber());
        transaction.setAmount(request.getAmount());
        transaction.setType(TransactionType.TRANSFER);
        transaction.setStatus(TransactionStatus.PROCESSING);
        transaction.setDescription(request.getDescription());
        Transaction savedTransaction = transactionRepository.save(transaction);
        log.info("Transaction saved as PROCESSING: {}", savedTransaction.getId());

        TransactionInitiatedEvent event = new TransactionInitiatedEvent();
        event.setTransactionId(savedTransaction.getId());
        event.setReceiverAccountNumber(savedTransaction.getReceiverAccountNumber());
        event.setSenderAccountNumber(savedTransaction.getSenderAccountNumber());
        event.setDescription(request.getDescription());
        event.setAmmount(savedTransaction.getAmount());

        kafkaTemplate.send(TRANSACTION_INITIARED_TOPIC,savedTransaction.getId(), event);
        log.info("SAGA STEP 2 - TransactionInitiatedEvent published:{}",savedTransaction.getId());
        return mapToResponse(savedTransaction);
    }

    private TransactionResponse mapToResponse(Transaction transaction) {

        if (transaction == null) {
            return null;
        }

        return new TransactionResponse(
                transaction.getId(),
                transaction.getSenderAccountNumber(),
                transaction.getReceiverAccountNumber(),
                transaction.getAmount(),
                transaction.getType(),
                transaction.getStatus(),
                transaction.getDescription(),
                transaction.getFailureReason(),
                transaction.getReferenceNumber(),
                transaction.getCreatedDate(),
                transaction.getCompletedAt()
        );
    }



    @Override
    public TransactionResponse getTransaction(String transactionId) {

        log.info("Fetching transaction details. transactionId={}", transactionId);

        if (transactionId == null || transactionId.isBlank()) {
            throw new IllegalArgumentException("Transaction id must not be null or empty.");
        }

        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> {
                    log.warn("Transaction not found. transactionId={}", transactionId);
                    return new RuntimeException(
                            "Transaction not found with id: " + transactionId);
                });

        log.info("Transaction fetched successfully. transactionId={}, status={}",
                transaction.getId(),
                transaction.getStatus());

        return mapToResponse(transaction);
    }
    @Override
    public List<TransactionResponse> getTransactionHistory(String accountNumber) {

        log.info("Fetching transaction history for account={}", accountNumber);

        if (accountNumber == null || accountNumber.isBlank()) {
            throw new IllegalArgumentException("Account number must not be null or empty.");
        }

        List<Transaction> transactions =
                transactionRepository.findBySenderAccountNumberOrderByCreatedAtDesc(accountNumber);

        if (transactions.isEmpty()) {
            log.info("No transactions found for account={}", accountNumber);
            return Collections.emptyList();
        }

        log.info("Found {} transaction(s) for account={}",
                transactions.size(),
                accountNumber);

        return transactions.stream()
                .map(this::mapToResponse)
                .toList();
    }


    @Override
    public TransactionResponse verifyOtp(String transactionId, String otp) {
        return null;
    }
}
