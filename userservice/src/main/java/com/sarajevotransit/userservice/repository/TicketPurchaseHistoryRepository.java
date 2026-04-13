package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketPurchaseHistoryRepository extends JpaRepository<TicketPurchaseHistoryEntry, Long> {

    List<TicketPurchaseHistoryEntry> findByUserIdOrderByPurchasedAtDesc(Long userId);

    Page<TicketPurchaseHistoryEntry> findByUserId(Long userId, Pageable pageable);
}
