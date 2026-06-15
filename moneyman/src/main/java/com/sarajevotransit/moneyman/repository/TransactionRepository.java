package com.sarajevotransit.moneyman.repository;

import com.sarajevotransit.moneyman.model.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    @Query("SELECT t FROM Transaction t LEFT JOIN FETCH t.ticket WHERE t.sagaId = :sagaId")
    Optional<Transaction> findBySagaIdWithTicket(String sagaId);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.status = com.sarajevotransit.moneyman.model.enums.PaymentStatus.COMPLETED AND t.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumCompletedRevenue(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(t) FROM Transaction t WHERE t.createdAt BETWEEN :startDate AND :endDate")
    long countInPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}
