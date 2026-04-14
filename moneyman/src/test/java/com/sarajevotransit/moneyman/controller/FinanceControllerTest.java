package com.sarajevotransit.moneyman.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarajevotransit.moneyman.dto.TicketPurchaseRequest;
import com.sarajevotransit.moneyman.dto.TicketResponseDTO;
import com.sarajevotransit.moneyman.model.enums.TicketStatus;
import com.sarajevotransit.moneyman.model.enums.TicketType;
import com.sarajevotransit.moneyman.service.MoneymanService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
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

    @Test
    void purchaseTicket_ValidRequest_ReturnsOk() throws Exception {
        // Setup DTO for response
        TicketResponseDTO response = new TicketResponseDTO();
        response.setId(UUID.randomUUID());
        response.setAmount(new BigDecimal("1.80"));
        response.setStatus(TicketStatus.ACTIVE);

        TicketPurchaseRequest request = new TicketPurchaseRequest();
        request.setUserId(1L);
        request.setTicketType(TicketType.SINGLE);
        request.setPaymentMethodId(1L);

        when(moneymanService.purchaseTicket(any())).thenReturn(response);

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
                .andExpect(status().isBadRequest());
    }

    @Test
    void getWallet_ReturnsList() throws Exception {
        TicketResponseDTO t1 = new TicketResponseDTO();
        t1.setId(UUID.randomUUID());
        t1.setQrCodeData("ST-123");

        when(moneymanService.getUserWallet(1L)).thenReturn(List.of(t1));

        mockMvc.perform(get("/api/finance/wallet/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].qrCodeData").value("ST-123"));
    }
}