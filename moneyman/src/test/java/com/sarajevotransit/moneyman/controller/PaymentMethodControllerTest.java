package com.sarajevotransit.moneyman.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sarajevotransit.moneyman.dto.PaymentMethodRequest;
import com.sarajevotransit.moneyman.model.PaymentMethod;
import com.sarajevotransit.moneyman.repository.PaymentMethodRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PaymentMethodController.class)
public class PaymentMethodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PaymentMethodRepository repository;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void getMethods_ReturnsList() throws Exception {
        PaymentMethod pm = new PaymentMethod();
        pm.setLastFour("4444");

        when(repository.findByUserId(1L)).thenReturn(List.of(pm));

        mockMvc.perform(get("/api/payments/methods/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lastFour").value("4444"));
    }

    @Test
    void addMethod_ReturnsCreatedMethod() throws Exception {
        PaymentMethodRequest request = PaymentMethodRequest.builder()
                .userId(1L)
                .provider("STRIPE")
                .gatewayToken("tok_123")
                .lastFour("4242")
                .cardType("VISA")
                .isDefault(true)
                .build();

        PaymentMethod pm = new PaymentMethod();
        pm.setId(1L);
        pm.setUserId(1L);
        pm.setProvider("STRIPE");
        pm.setLastFour("4242");
        pm.setCardType("VISA");
        pm.setDefault(true);

        when(repository.save(any())).thenReturn(pm);

        mockMvc.perform(post("/api/payments/methods")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("STRIPE"))
                .andExpect(jsonPath("$.lastFour").value("4242"));
    }

    @Test
    void deleteMethod_ReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/payments/methods/1"))
                .andExpect(status().isNoContent());
    }
}