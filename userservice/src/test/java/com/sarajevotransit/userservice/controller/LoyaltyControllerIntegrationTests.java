package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.ThemeMode;
import com.sarajevotransit.userservice.repository.UserProfileRepository;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class LoyaltyControllerIntegrationTests {

        private MockMvc mockMvc;

        @Autowired
        private WebApplicationContext webApplicationContext;

        @Autowired
        private UserProfileRepository userProfileRepository;

        @Autowired
        private UserService userService;

        @BeforeEach
        void setUp() {
                mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
                userProfileRepository.deleteAll();
        }

        @Test
        void earnPoints_thenBalanceShouldReflectChange() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Tarik Husic",
                                "tarik.husic@sarajevotransit.ba",
                                "TarikPass123",
                                LanguageCode.EN,
                                ThemeMode.SYSTEM,
                                NotificationChannel.EMAIL));

                String payload = """
                                {
                                  "points": 50,
                                  "description": "Ticket purchase bonus",
                                  "referenceType": "ticket_purchase"
                                }
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/loyalty/earn", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.userId").value(user.id()))
                                .andExpect(jsonPath("$.currentBalance").value(50));

                mockMvc.perform(get("/api/v1/users/{userId}/loyalty/balance", user.id()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.currentBalance").value(50));
        }

        @Test
        void redeemPoints_withoutEnoughBalance_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Mina Alic",
                                "mina.alic@sarajevotransit.ba",
                                "MinaPass123",
                                LanguageCode.BS,
                                ThemeMode.DARK,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                  "points": 10,
                                  "description": "Discount",
                                  "referenceType": "discount"
                                }
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/loyalty/redeem", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message")
                                                .value("User does not have enough loyalty points for redemption."));
        }

        @Test
        void earnPoints_withMalformedJson_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Nedim Hasic",
                                "nedim.hasic@sarajevotransit.ba",
                                "NedimPass123",
                                LanguageCode.BS,
                                ThemeMode.LIGHT,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                  "points": 10,
                                  "description": "Broken",
                                  "referenceType": "ticket_purchase"
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/loyalty/earn", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Malformed JSON request"));
        }

        @Test
        void getBalance_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/users/0/loyalty/balance"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void getTransactions_shouldReturnPaginatedContent() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Lejla Music",
                                "lejla.music@sarajevotransit.ba",
                                "LejlaPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                  "points": 10,
                                  "description": "Ride bonus",
                                  "referenceType": "ticket_purchase"
                                }
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/loyalty/earn", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated());

                mockMvc.perform(post("/api/v1/users/{userId}/loyalty/earn", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated());

                mockMvc.perform(get("/api/v1/users/{userId}/loyalty/transactions", user.id())
                                .queryParam("page", "0")
                                .queryParam("size", "1")
                                .queryParam("sort", "createdAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.size").value(1))
                                .andExpect(jsonPath("$.totalElements").value(2));
        }
}
