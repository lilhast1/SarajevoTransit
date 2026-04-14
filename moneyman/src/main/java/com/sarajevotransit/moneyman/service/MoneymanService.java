package com.sarajevotransit.moneyman.service;

import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
import com.sarajevotransit.moneyman.model.*;
import com.sarajevotransit.moneyman.model.enums.*;
import com.sarajevotransit.moneyman.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
//@RequiredArgsConstructor
public class MoneymanService {

    private final TicketRepository ticketRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final MoneymanMapper mapper;

    public MoneymanService(TicketRepository ticketRepository, TransactionRepository transactionRepository, PaymentMethodRepository paymentMethodRepository, MoneymanMapper mapper) {
        this.ticketRepository = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.mapper = mapper;
    }
    private static final Logger logger = LoggerFactory.getLogger(MoneymanService.class);
    @Transactional
    public Ticket purchaseTicket(TicketPurchaseRequest request) {
        // 1. F11: Verify tokenized payment method exists
        PaymentMethod pm = paymentMethodRepository.findById(request.getPaymentMethodId())
                .filter(p -> p.getUserId().equals(request.getUserId()))
                .orElseThrow(() -> new RuntimeException("Valid payment method not found"));

        // 2. F3 Logic: Determine Price and Duration
        BigDecimal price = calculatePrice(request.getTicketType());
        LocalDateTime expiry = calculateExpiry(request.getTicketType());

        // 3. F11: Simulate External Gateway (Stripe/PayPal) call using the token
        logger.info("Charging card ending in {} via {} token: {}", pm.getLastFour(), pm.getProvider(), pm.getGatewayToken());
        String externalId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);

        // 4. F11: Create Transaction History
        Transaction tx = new Transaction();
        tx.setUserId(request.getUserId());
        tx.setAmount(price);
        tx.setCurrency("BAM");
        tx.setStatus(PaymentStatus.COMPLETED);
        tx.setExternalTransactionId(externalId);
        tx.setCreatedAt(LocalDateTime.now());
        transactionRepository.save(tx);

        // 5. F3: Create Ticket for Digital Wallet
        Ticket ticket = new Ticket();
        ticket.setUserId(request.getUserId());
        ticket.setType(request.getTicketType());
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setValidFrom(LocalDateTime.now());
        ticket.setValidUntil(expiry);

        // F3: Generate unique QR Data for vehicle validation
        ticket.setQrCodeData("ST-" + UUID.randomUUID());
        ticket.setTransaction(tx);

        Ticket savedTicket = ticketRepository.save(ticket);

        // F3: Post-condition - Send confirmation (Mock)
        sendEmailConfirmation(request.getUserId(), savedTicket);

        return savedTicket;
    }

    private BigDecimal calculatePrice(TicketType type) {
        return switch (type) {
            case SINGLE -> new BigDecimal("1.80");
            case DAILY -> new BigDecimal("5.00");
            case WEEKLY -> new BigDecimal("20.00");
            case MONTHLY -> new BigDecimal("50.00");
        };
    }

    private LocalDateTime calculateExpiry(TicketType type) {
        LocalDateTime now = LocalDateTime.now();
        return switch (type) {
            case SINGLE -> now.plusHours(1);
            case DAILY -> now.plusDays(1);
            case WEEKLY -> now.plusWeeks(1);
            case MONTHLY -> now.plusMonths(1);
        };
    }

    private void sendEmailConfirmation(Long userId, Ticket ticket) {
        logger.info("F3: Email confirmation sent to User {} for Ticket {}", userId, ticket.getId());
    }

    // MoneymanService.java

    public List<TicketResponseDTO> getUserWallet(Long userId) {
        return ticketRepository.findAllByUserIdWithTransaction(userId)
                .stream()
                .map(mapper::toResponseDTO) // This "breaks" the cycle
                .toList();
    }
}