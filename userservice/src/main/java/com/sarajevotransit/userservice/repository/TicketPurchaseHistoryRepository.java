package com.sarajevotransit.userservice.repository;

import com.sarajevotransit.userservice.model.TicketPurchaseHistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketPurchaseHistoryRepository extends JpaRepository<TicketPurchaseHistoryEntry, Long> {

    List<TicketPurchaseHistoryEntry> findByUserIdOrderByPurchasedAtDesc(Long userId);
}
