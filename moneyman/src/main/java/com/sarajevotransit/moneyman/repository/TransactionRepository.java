package com.sarajevotransit.moneyman.repository;

import com.sarajevotransit.moneyman.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
}