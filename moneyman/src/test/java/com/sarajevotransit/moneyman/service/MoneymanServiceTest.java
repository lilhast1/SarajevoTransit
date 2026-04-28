package com.sarajevotransit.moneyman.service;

import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
import com.sarajevotransit.moneyman.model.PaymentMethod;
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
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
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
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
            Ticket ticket = invocation.getArgument(0);
            ticket.setId(UUID.randomUUID());
            return ticket;
        });

        TicketPurchaseRequest request = new TicketPurchaseRequest(1L, TicketType.SINGLE, 1L);
        Ticket savedTicket = moneymanService.purchaseTicket(request);

        assertNotNull(savedTicket);
        assertEquals(TicketStatus.ACTIVE, savedTicket.getStatus());
        assertNotNull(savedTicket.getTransaction());
        assertEquals(new BigDecimal("1.80"), savedTicket.getTransaction().getAmount());
    }

    @Test
    void purchaseTicket_InvalidPaymentMethod_ThrowsResourceNotFoundException() {
        when(paymentMethodRepository.findById(1L)).thenReturn(Optional.empty());

        TicketPurchaseRequest request = new TicketPurchaseRequest(1L, TicketType.SINGLE, 1L);

        Exception exception = assertThrows(RuntimeException.class, () -> moneymanService.purchaseTicket(request));
        assertTrue(exception.getMessage().contains("Valid payment method not found"));
    }
}
