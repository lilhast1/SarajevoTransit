package com.sarajevotransit.moneyman.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.mapper.MoneymanMapper;
// import com.sarajevotransit.moneyman.mapper.MoneymanMapperImpl;
import com.sarajevotransit.moneyman.model.Ticket;
import com.sarajevotransit.moneyman.model.Transaction;
import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.model.enums.TicketType;
import com.sarajevotransit.moneyman.service.MoneymanService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FinanceController.class)
public class FinanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MoneymanService moneymanService;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MoneymanMapper moneymanMapper;

    @Test
    void purchaseTicket_ValidRequest_ReturnsOk() throws Exception {
        // Mock domain entity
        Ticket ticket = new Ticket();
        ticket.setId(UUID.randomUUID());
        Transaction transaction = new Transaction();
        transaction.setAmount(new BigDecimal("1.80"));
        transaction.setCurrency("BAM");
        transaction.setExternalTransactionId("TX-123");
        ticket.setTransaction(transaction);
        ticket.setStatus(TicketStatus.ACTIVE);

        // Mock DTO
        TicketResponseDTO response = new TicketResponseDTO();
        response.setId(ticket.getId());
        response.setAmount(ticket.getTransaction().getAmount());
        response.setStatus(ticket.getStatus());

        TicketPurchaseRequest request = new TicketPurchaseRequest();
        request.setUserId(1L);
        request.setTicketType(TicketType.SINGLE);
        request.setPaymentMethodId(1L);

        // FIX: service returns Ticket
        when(moneymanService.purchaseTicket(any())).thenReturn(ticket);

        // FIX: mapper converts → DTO
        when(moneymanMapper.toResponseDTO(ticket)).thenReturn(response);

        mockMvc.perform(post("/api/finance/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.amount").value(1.80));
    }

    @Test
    void purchaseTicket_InvalidRequest_ReturnsBadRequest() throws Exception {
        // Missing ticketType and paymentMethodId
        TicketPurchaseRequest request = new TicketPurchaseRequest();
        request.setUserId(1L);

        mockMvc.perform(post("/api/finance/purchase")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("validation"))
                .andExpect(jsonPath("$.fieldErrors.ticketType").value("Ticket Type is required"));
    }

    @Test
    void getWallet_ReturnsList() throws Exception {
        TicketResponseDTO t1 = new TicketResponseDTO();
        t1.setId(UUID.randomUUID());
        t1.setQrCodeData("ST-123");

        Page<TicketResponseDTO> page = new PageImpl<>(List.of(t1));
        when(moneymanService.getUserWallet(eq(1L), any(Pageable.class))).thenReturn(page);

        mockMvc.perform(get("/api/finance/wallet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].qrCodeData").value("ST-123"));
    }
}