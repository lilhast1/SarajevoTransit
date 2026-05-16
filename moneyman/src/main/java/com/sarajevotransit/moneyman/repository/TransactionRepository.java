package com.sarajevotransit.moneyman.repository;

import com.sarajevotransit.moneyman.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.ticket WHERE t.sagaId = :sagaId")
    Optional<Transaction> findBySagaIdWithTicket(String sagaId);
}
