package com.banking.transaction.service.entity;

/**
 * Transaction LifeCycle Flow:
 *
 * PENDING->PROCESSING-> COMPLETED (clean transaction)
 *                     ->PENDING_VERIFICATION(suspicious detected)
 *                               ->COMPLETED(verified)
 *                               ->FLAGGED(SAGE REFUND)
 *                      ->FAILED
 *                      ->FLAGGED
 */
public enum TransactionStatus {
    PENDING,
    COMPLETED,
    FAILED,
    PENDING_VERIFICATION,
    PROCESSING,
    FLAGGED
}
