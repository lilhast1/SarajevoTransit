package com.sarajevotransit.moneyman.service;

import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketValidationResponse;
import com.sarajevotransit.moneyman.exception.PaymentFailedException;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
import com.sarajevotransit.moneyman.model.PaymentMethod;
import com.sarajevotransit.moneyman.saga.TicketSagaPublisher;
import com.sarajevotransit.moneyman.model.Ticket;
import com.sarajevotransit.moneyman.model.Transaction;
import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.model.enums.TicketType;
import com.sarajevotransit.moneyman.model.enums.PaymentStatus;
import com.sarajevotransit.moneyman.repository.PaymentMethodRepository;
import com.sarajevotransit.moneyman.repository.TicketRepository;
import com.sarajevotransit.moneyman.repository.TransactionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MoneymanServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private PaymentMethodRepository paymentMethodRepository;

    @Mock
    private MoneymanMapper mapper;

    @Mock
    private TicketSagaPublisher sagaPublisher;

    @Mock
    private StripePaymentService stripePaymentService;

    @InjectMocks
    private MoneymanService moneymanService;

    @Test
    void purchaseTicket_ValidRequest_ReturnsSavedTicket() {
        PaymentMethod method = new PaymentMethod();
        method.setId(1L);
        method.setUserId(1L);
        method.setProvider("STRIPE");
        method.setLastFour("4242");
        method.setGatewayToken("tok_123");

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(method));
        when(stripePaymentService.charge(any(BigDecimal.class), any())).thenReturn("pi_test_123");
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(UUID.randomUUID());
            return ticket;
        });

        TicketPurchaseRequest request = new TicketPurchaseRequest(1L, TicketType.SINGLE, 1L, null, null);
        Ticket savedTicket = moneymanService.purchaseTicket(request);

        assertNotNull(savedTicket);
        assertEquals(TicketStatus.PENDING, savedTicket.getStatus());
        assertNotNull(savedTicket.getTransaction());
        assertEquals(new BigDecimal("1.80"), savedTicket.getTransaction().getAmount());
        assertEquals("pi_test_123", savedTicket.getTransaction().getExternalTransactionId());
    }

    @Test
    void purchaseTicket_CardDeclined_ThrowsAndPersistsNothing() {
        PaymentMethod method = new PaymentMethod();
        method.setId(1L);
        method.setUserId(1L);
        method.setProvider("STRIPE");
        method.setGatewayToken("pm_card_visa_chargeDeclined");

        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.of(method));
        when(stripePaymentService.charge(any(BigDecimal.class), any()))
                .thenThrow(new PaymentFailedException("Your card was declined."));

        TicketPurchaseRequest request = new TicketPurchaseRequest(1L, TicketType.SINGLE, 1L);

        assertThrows(PaymentFailedException.class, () -> moneymanService.purchaseTicket(request));
        verify(transactionRepository, never()).save(any());
        verify(ticketRepository, never()).save(any());
    }

    @Test
    void validateTicket_ActiveSingle_MarksUsedAndReturnsValid() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setType(TicketType.SINGLE);
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setValidUntil(LocalDateTime.now().plusHours(1));

        when(ticketRepository.findByQrCodeData("ST-abc")).thenReturn(Optional.of(ticket));

        TicketValidationResponse res = moneymanService.validateTicket("ST-abc");

        assertTrue(res.isValid());
        assertEquals(TicketStatus.USED, res.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void validateTicket_NotFound_ReturnsInvalid() {
        when(ticketRepository.findByQrCodeData("missing")).thenReturn(Optional.empty());

        TicketValidationResponse res = moneymanService.validateTicket("missing");

        assertFalse(res.isValid());
        assertEquals("Ticket not found", res.getMessage());
    }

    @Test
    void validateTicket_Expired_MarksExpiredAndReturnsInvalid() {
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        ticket.setType(TicketType.DAILY);
        ticket.setStatus(TicketStatus.ACTIVE);
        ticket.setValidUntil(LocalDateTime.now().minusMinutes(5));

        when(ticketRepository.findByQrCodeData("ST-old")).thenReturn(Optional.of(ticket));

        TicketValidationResponse res = moneymanService.validateTicket("ST-old");

        assertFalse(res.isValid());
        assertEquals(TicketStatus.EXPIRED, res.getStatus());
        verify(ticketRepository).save(ticket);
    }

    @Test
    void purchaseTicket_InvalidPaymentMethod_ThrowsResourceNotFoundException() {
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

        TicketPurchaseRequest request = new TicketPurchaseRequest(1L, TicketType.SINGLE, 1L, null, null);

        Exception exception = assertThrows(RuntimeException.class, () -> moneymanService.purchaseTicket(request));
        assertTrue(exception.getMessage().contains("Valid payment method not found"));
    }
}
