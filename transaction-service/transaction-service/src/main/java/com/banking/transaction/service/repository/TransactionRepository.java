package com.banking.transaction.service.repository;

import com.banking.transaction.service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
@Repository
public interface TransactionRepository extends JpaRepository<Transaction,String> {

    List<Transaction>
    findBySenderAccountNumberOrderByCreatedDateDesc(
            String senderAccountNumber
    );
}
