package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.dto.TicketPurchaseStatsResponse;
import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface TicketPurchaseHistoryRepository extends JpaRepository<TicketPurchaseHistoryEntry, Long> {

    List<TicketPurchaseHistoryEntry> findByUserIdOrderByPurchasedAtDesc(Long userId);

    Page<TicketPurchaseHistoryEntry> findByUserId(Long userId, Pageable pageable);

    @Query("""
            select new com.sarajevotransit.userservice.dto.TicketPurchaseStatsResponse(
                t.ticketType,
                count(t.id),
                coalesce(sum(t.amount), 0)
            )
            from TicketPurchaseHistoryEntry t
            where t.user.id = :userId
            group by t.ticketType
            order by coalesce(sum(t.amount), 0) desc
            """)
    List<TicketPurchaseStatsResponse> findTicketPurchaseStatsByUserId(Long userId);
}
