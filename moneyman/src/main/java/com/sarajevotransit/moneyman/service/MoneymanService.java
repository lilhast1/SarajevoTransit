package com.sarajevotransit.moneyman.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
import com.sarajevotransit.moneyman.exception.ResourceNotFoundException;
import com.sarajevotransit.moneyman.model.*;
import com.sarajevotransit.moneyman.model.enums.*;
import com.sarajevotransit.moneyman.repository.*;
import com.sarajevotransit.moneyman.saga.TicketSagaPublisher;
import com.sarajevotransit.moneyman.saga.event.TicketPurchaseInitiatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
public class MoneymanService {

    private final TicketRepository ticketRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final MoneymanMapper mapper;
    private final ObjectMapper objectMapper;
    private final TicketSagaPublisher sagaPublisher;

    public MoneymanService(TicketRepository ticketRepository, TransactionRepository transactionRepository,
            PaymentMethodRepository paymentMethodRepository, MoneymanMapper mapper, ObjectMapper objectMapper,
            TicketSagaPublisher sagaPublisher) {
        this.ticketRepository = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.sagaPublisher = sagaPublisher;
    }

    @Transactional
    public Ticket purchaseTicket(TicketPurchaseRequest request) {
        PaymentMethod pm = paymentMethodRepository.findById(request.getPaymentMethodId())
                .filter(p -> p.getUserId().equals(request.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Valid payment method not found"));

        BigDecimal price = calculatePrice(request.getTicketType());
        LocalDateTime expiry = calculateExpiry(request.getTicketType());

        log.info("Charging card ending in {} via {} token: {}", pm.getLastFour(), pm.getProvider(),
                pm.getGatewayToken());
        String externalId = "PAY-" + UUID.randomUUID().toString().substring(0, 8);
        String sagaId = UUID.randomUUID().toString();

        // Local TX 1: create Transaction(PENDING) + Ticket(PENDING)
        Transaction tx = new Transaction();
        tx.setUserId(request.getUserId());
        tx.setAmount(price);
        tx.setCurrency("BAM");
        tx.setStatus(PaymentStatus.PENDING);
        tx.setExternalTransactionId(externalId);
        tx.setCreatedAt(LocalDateTime.now());
        tx.setSagaId(sagaId);
        transactionRepository.save(tx);

        Ticket ticket = new Ticket();
        ticket.setUserId(request.getUserId());
        ticket.setType(request.getTicketType());
        ticket.setStatus(TicketStatus.PENDING);
        ticket.setPurchaseDate(LocalDateTime.now());
        ticket.setValidFrom(LocalDateTime.now());
        ticket.setValidUntil(expiry);
        ticket.setQrCodeData("ST-" + UUID.randomUUID());
        ticket.setTransaction(tx);
        Ticket savedTicket = ticketRepository.save(ticket);

        // Publish event — UserService will record purchase history + earn loyalty points
        sagaPublisher.publishPurchaseInitiated(new TicketPurchaseInitiatedEvent(
                sagaId,
                request.getUserId(),
                savedTicket.getId(),
                tx.getId(),
                request.getTicketType().name(),
                price,
                externalId
        ));

        log.info("Saga [{}]: TicketPurchaseInitiated published for user {}", sagaId, request.getUserId());
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

    public Page<TicketResponseDTO> getUserWallet(Long userId, Pageable pageable) {
        return ticketRepository.findAllByUserIdWithTransaction(userId, pageable)
                .map(mapper::toResponseDTO);
    }

    @Transactional
    public Ticket updateTicket(UUID ticketId, JsonPatch patch) throws JsonPatchException, IllegalArgumentException, JsonProcessingException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        TicketStatus previousStatus = ticket.getStatus();

        JsonNode ticketNode = objectMapper.valueToTree(ticket);
        JsonNode patchedNode = patch.apply(ticketNode);
        Ticket patchedTicket = objectMapper.treeToValue(patchedNode, Ticket.class);

        Ticket savedTicket = ticketRepository.save(patchedTicket);
        if (previousStatus != TicketStatus.USED && savedTicket.getStatus() == TicketStatus.USED) {
            sagaPublisher.publishRideValidated(new com.sarajevotransit.moneyman.saga.event.TicketRideValidatedEvent(
                    savedTicket.getUserId(),
                    savedTicket.getId(),
                    savedTicket.getType() != null ? savedTicket.getType().name() : "UNKNOWN",
                    1
            ));
        }

        return savedTicket;
    }
}
