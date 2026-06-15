package com.sarajevotransit.moneyman.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;
import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.dto.TicketValidationResponse;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
import com.sarajevotransit.moneyman.exception.ResourceNotFoundException;
import com.sarajevotransit.moneyman.client.LoyaltyCouponClient;
import com.sarajevotransit.moneyman.dto.CouponApplicationResponse;
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
    private final StripePaymentService stripePaymentService;
    private final LoyaltyCouponClient loyaltyCouponClient;

    public MoneymanService(TicketRepository ticketRepository, TransactionRepository transactionRepository,
            PaymentMethodRepository paymentMethodRepository, MoneymanMapper mapper, ObjectMapper objectMapper,
            TicketSagaPublisher sagaPublisher, StripePaymentService stripePaymentService,
            LoyaltyCouponClient loyaltyCouponClient) {
        this.ticketRepository = ticketRepository;
        this.transactionRepository = transactionRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.mapper = mapper;
        this.objectMapper = objectMapper;
        this.sagaPublisher = sagaPublisher;
        this.stripePaymentService = stripePaymentService;
        this.loyaltyCouponClient = loyaltyCouponClient;
    }

    @Transactional
    public Ticket purchaseTicket(TicketPurchaseRequest request) {
        PaymentMethod pm = paymentMethodRepository.findById(request.getPaymentMethodId())
                .filter(p -> p.getUserId().equals(request.getUserId()))
                .orElseThrow(() -> new ResourceNotFoundException("Valid payment method not found"));

        BigDecimal price = calculatePrice(request.getTicketType());
        LocalDateTime expiry = calculateExpiry(request.getTicketType());

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            CouponApplicationResponse coupon = loyaltyCouponClient.applyCoupon(
                    request.getUserId(),
                    request.getCouponCode().trim(),
                    request.getRideCode());

            if (coupon.freeRide()) {
                price = BigDecimal.ZERO;
            } else if (coupon.discountPercent() > 0) {
                price = price.multiply(BigDecimal.valueOf(100 - coupon.discountPercent()))
                        .divide(BigDecimal.valueOf(100), 2, java.math.RoundingMode.HALF_UP);
            }
        }

        // Charge the card via Stripe first ? a decline throws PaymentFailedException (HTTP 402)
        // and nothing is persisted. The returned PaymentIntent id is our external transaction id.
        log.info("Charging card ending in {} via {} token: {}", pm.getLastFour(), pm.getProvider(),
                pm.getGatewayToken());
        String externalId = stripePaymentService.charge(price, pm.getGatewayToken());
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

        // Publish event ? UserService will record purchase history + earn loyalty points
        sagaPublisher.publishPurchaseInitiated(new TicketPurchaseInitiatedEvent(
                sagaId,
                request.getUserId(),
                savedTicket.getId(),
                tx.getId(),
                request.getTicketType().name(),
                price,
                externalId));

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

    /**
     * Validates a ticket by its QR code when scanned upon boarding. A SINGLE ticket is consumed
     * (marked USED) on a successful validation; multi-ride passes (DAILY/WEEKLY/MONTHLY) stay ACTIVE.
     * Tickets past their validity are lazily marked EXPIRED here as well.
     */
    @Transactional
    public TicketValidationResponse validateTicket(String qrCodeData) {
        Ticket ticket = ticketRepository.findByQrCodeData(qrCodeData).orElse(null);
        if (ticket == null) {
            return TicketValidationResponse.builder()
                    .valid(false)
                    .message("Ticket not found")
                    .build();
        }

        if (ticket.getValidUntil() != null && ticket.getValidUntil().isBefore(LocalDateTime.now())) {
            if (ticket.getStatus() == TicketStatus.ACTIVE) {
                ticket.setStatus(TicketStatus.EXPIRED);
                ticketRepository.save(ticket);
            }
            return buildValidationResponse(ticket, false, "Ticket expired");
        }

        if (ticket.getStatus() != TicketStatus.ACTIVE) {
            return buildValidationResponse(ticket, false, "Ticket is not active (status: " + ticket.getStatus() + ")");
        }

        // Valid ride. Consume single-ride tickets; passes remain active for further boardings.
        if (ticket.getType() == TicketType.SINGLE) {
            ticket.setStatus(TicketStatus.USED);
            ticketRepository.save(ticket);
        }
        log.info("Ticket {} validated successfully ({})", ticket.getId(), ticket.getType());
        return buildValidationResponse(ticket, true, "Valid ticket");
    }

    private TicketValidationResponse buildValidationResponse(Ticket ticket, boolean valid, String message) {
        return TicketValidationResponse.builder()
                .valid(valid)
                .ticketId(ticket.getId())
                .type(ticket.getType())
                .status(ticket.getStatus())
                .validUntil(ticket.getValidUntil())
                .message(message)
                .build();
    }

    @Transactional
    public Ticket updateTicket(UUID ticketId, JsonPatch patch)
            throws JsonPatchException, IllegalArgumentException, JsonProcessingException {
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
                    1));
        }

        return savedTicket;
    }
}
