package com.sarajevotransit.moneyman.repository;

import com.sarajevotransit.moneyman.model.Ticket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID>, PagingAndSortingRepository<Ticket, UUID> {

    // N+1 Solution: Fetch transaction in the same query
    @Query("SELECT t FROM Ticket t JOIN FETCH t.transaction WHERE t.userId = :userId")
    List<Ticket> findAllByUserIdWithTransaction(Long userId);

    @Query("SELECT t FROM Ticket t JOIN FETCH t.transaction WHERE t.userId = :userId")
    Page<Ticket> findAllByUserIdWithTransaction(Long userId, Pageable pageable);

    @Modifying
    @Query("UPDATE Ticket t SET t.status = 'EXPIRED' WHERE t.status = 'ACTIVE' AND t.validUntil < :now")
    int deactivateExpiredTickets(LocalDateTime now);

    @Query("SELECT COUNT(t) FROM Ticket t WHERE t.status = com.sarajevotransit.moneyman.model.enums.TicketStatus.ACTIVE")
    long countActive();

    @Query("SELECT t.type AS type, COUNT(t) AS cnt FROM Ticket t WHERE t.purchaseDate BETWEEN :startDate AND :endDate GROUP BY t.type")
    List<Object[]> countByTypeInPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT t.status AS status, COUNT(t) AS cnt FROM Ticket t WHERE t.purchaseDate BETWEEN :startDate AND :endDate GROUP BY t.status")
    List<Object[]> countByStatusInPeriod(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT TO_CHAR(t.purchase_date, 'YYYY-MM-DD') AS day,
                   COUNT(*) AS cnt,
                   COALESCE(SUM(tr.amount), 0) AS revenue
            FROM tickets t
            JOIN transactions tr ON tr.id = t.transaction_id
            WHERE tr.status = 'COMPLETED'
              AND t.purchase_date BETWEEN :startDate AND :endDate
            GROUP BY TO_CHAR(t.purchase_date, 'YYYY-MM-DD')
            ORDER BY day
            """, nativeQuery = true)
    List<Object[]> revenueTimeSeries(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT CAST(EXTRACT(HOUR FROM t.purchase_date) AS integer) AS hour, COUNT(*) AS cnt
            FROM tickets t
            WHERE t.purchase_date BETWEEN :startDate AND :endDate
            GROUP BY CAST(EXTRACT(HOUR FROM t.purchase_date) AS integer)
            ORDER BY hour
            """, nativeQuery = true)
    List<Object[]> hourlyDistribution(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query(value = """
            SELECT t.user_id AS userId, COUNT(*) AS ticketCount, COALESCE(SUM(tr.amount), 0) AS totalSpent
            FROM tickets t
            JOIN transactions tr ON tr.id = t.transaction_id
            WHERE tr.status = 'COMPLETED'
              AND t.purchase_date BETWEEN :startDate AND :endDate
            GROUP BY t.user_id
            ORDER BY ticketCount DESC
            LIMIT 5
            """, nativeQuery = true)
    List<Object[]> topBuyers(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
}