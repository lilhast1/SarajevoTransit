package com.sarajevotransit.moneyman.worker;

import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.repository.TicketRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExpirationWorker {

    private final TicketRepository ticketRepository;

    // Runs every 5 minutes
    @Scheduled(fixedRate = 300000)
    @Transactional
    public void deactivateExpiredTickets() {
        log.info("Checking for expired tickets...");
        // This is a bulk update query you should add to your Repository
        int count = ticketRepository.deactivateExpiredTickets(LocalDateTime.now());
        if (count > 0) {
            log.info("F3: Automatically deactivated {} expired tickets.", count);
        }
    }
}