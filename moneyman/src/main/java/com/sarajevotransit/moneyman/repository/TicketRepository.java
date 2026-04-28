package com.sarajevotransit.moneyman.repository;

import com.sarajevotransit.moneyman.model.Ticket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    // N+1 Solution: Fetch transaction in the same query
    @Query("SELECT t FROM Ticket t JOIN FETCH t.transaction WHERE t.userId = :userId")
    List<Ticket> findAllByUserIdWithTransaction(Long userId);
    @Modifying
    @Query("UPDATE Ticket t SET t.status = 'EXPIRED' WHERE t.status = 'ACTIVE' AND t.validUntil < :now")
    int deactivateExpiredTickets(LocalDateTime now);
}