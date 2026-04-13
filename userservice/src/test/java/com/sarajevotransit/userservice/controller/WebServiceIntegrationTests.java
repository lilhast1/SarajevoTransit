package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.dto.LoyaltyEarnRequest;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.model.TicketType;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
import com.sarajevotransit.userservice.service.LoyaltyService;
import com.sarajevotransit.userservice.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class WebServiceIntegrationTests {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private LoyaltyService loyaltyService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
        userProfileRepository.deleteAll();
    }

    @Test
    void updatePreference_shouldReturnUpdatedPreference() throws Exception {
        var user = userService.createUser(new CreateUserRequest(
                "Amina Hadzic",
                "amina.hadzic@sarajevotransit.ba",
                "AminaPass123",
                LanguageCode.BS,
                ThemeMode.SYSTEM,
                NotificationChannel.PUSH));

        String payload = """
                {
                  "languageCode": "EN",
                  "themeMode": "DARK",
                  "notificationChannel": "EMAIL",
                  "highContrastEnabled": true,
                  "largeTextEnabled": false,
                  "screenReaderEnabled": true
                }
                """;

        mockMvc.perform(put("/api/v1/users/{userId}/preferences", user.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.languageCode").value("EN"))
                .andExpect(jsonPath("$.themeMode").value("DARK"))
                .andExpect(jsonPath("$.notificationChannel").value("EMAIL"))
                .andExpect(jsonPath("$.highContrastEnabled").value(true))
                .andExpect(jsonPath("$.screenReaderEnabled").value(true));
    }

    @Test
    void addTicketPurchase_shouldReturnCreatedEntry() throws Exception {
        var user = userService.createUser(new CreateUserRequest(
                "Tarik Kovac",
                "tarik.kovac@sarajevotransit.ba",
                "TarikPass123",
                LanguageCode.EN,
                ThemeMode.DARK,
                NotificationChannel.EMAIL));

        String payload = """
                {
                  "ticketType": "MONTHLY",
                  "amount": 53.00,
                  "paymentMethod": "CARD",
                  "externalTransactionId": "TXN-TARIK-0099",
                  "lineCode": "TRAM-3"
                }
                """;

        mockMvc.perform(post("/api/v1/users/{userId}/ticket-purchases", user.id())
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticketType").value("MONTHLY"))
                .andExpect(jsonPath("$.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.externalTransactionId").value("TXN-TARIK-0099"));
    }

    @Test
    void getUserSummary_shouldReturnAggregatedData() throws Exception {
        var user = userService.createUser(new CreateUserRequest(
                "Lejla Music",
                "lejla.music@sarajevotransit.ba",
                "LejlaPass123",
                LanguageCode.BS,
                ThemeMode.LIGHT,
                NotificationChannel.PUSH));

        userService.addTravelHistory(user.id(), new AddTravelHistoryRequest(
                "TRAM-3",
                "Skenderija",
                "Bascarsija",
                LocalDateTime.now().minusHours(5),
                16));

        userService.addTicketPurchase(user.id(), new AddTicketPurchaseRequest(
                TicketType.DAILY,
                new BigDecimal("2.00"),
                "CARD",
                "TXN-LEJLA-0001",
                "TRAM-3",
                LocalDateTime.now().minusHours(4)));

        loyaltyService.earnPoints(user.id(), new LoyaltyEarnRequest(
                15,
                "Daily ticket bonus",
                "ticket_purchase"));

        mockMvc.perform(get("/api/v1/users/{userId}/summary", user.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.profile.id").value(user.id()))
                .andExpect(jsonPath("$.travelHistory.length()").value(1))
                .andExpect(jsonPath("$.ticketPurchases.length()").value(1))
                .andExpect(jsonPath("$.loyaltyTransactions.length()").value(1));
    }

    @Test
    void getPreference_forMissingUser_shouldReturnNotFound() throws Exception {
        mockMvc.perform(get("/api/v1/users/999/preferences"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("User with id 999 not found."));
    }
}
