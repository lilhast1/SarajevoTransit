package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.AddTicketPurchaseRequest;
import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
import com.sarajevotransit.userservice.dto.CreateUserRequest;
import com.sarajevotransit.userservice.model.LanguageCode;
import com.sarajevotransit.userservice.model.NotificationChannel;
import com.sarajevotransit.userservice.model.TicketType;
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

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
class UserControllerIntegrationTests {

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
        void createUser_shouldReturnCreated() throws Exception {
                String payload = """
                                {
                                  "fullName": "Lejla Music",
                                  "email": "LEJLA.MUSIC@SARAJEVOTRANSIT.BA",
                                  "password": "StrongPass123",
                                  "languageCode": "BS",
                                  "themeMode": "SYSTEM",
                                  "notificationChannel": "PUSH"
                                }
                                """;

                mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.fullName").value("Lejla Music"))
                                .andExpect(jsonPath("$.email").value("lejla.music@sarajevotransit.ba"));
        }

        @Test
        void createUser_withInvalidPayload_shouldReturnBadRequest() throws Exception {
                String payload = """
                                {
                                  "fullName": "",
                                  "email": "invalid-email",
                                  "password": "123"
                                }
                                """;

                mockMvc.perform(post("/api/v1/users")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        void getUser_shouldReturnExistingUser() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Amina Selimovic",
                                "amina.selimovic@sarajevotransit.ba",
                                "AminaPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                mockMvc.perform(get("/api/v1/users/{userId}", user.id()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(user.id()))
                                .andExpect(jsonPath("$.email").value("amina.selimovic@sarajevotransit.ba"));
        }

        @Test
        void getPreference_shouldReturnCurrentPreference() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Nina Basic",
                                "nina.basic@sarajevotransit.ba",
                                "NinaPass123",
                                LanguageCode.EN,
                                ThemeMode.DARK,
                                NotificationChannel.EMAIL));

                mockMvc.perform(get("/api/v1/users/{userId}/preferences", user.id()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.languageCode").value("EN"))
                                .andExpect(jsonPath("$.themeMode").value("DARK"))
                                .andExpect(jsonPath("$.notificationChannel").value("EMAIL"));
        }

        @Test
        void getTravelHistory_shouldReturnStoredEntries() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Ahmed Becic",
                                "ahmed.becic@sarajevotransit.ba",
                                "AhmedPass123",
                                LanguageCode.BS,
                                ThemeMode.DARK,
                                NotificationChannel.EMAIL));

                userService.addTravelHistory(user.id(), new AddTravelHistoryRequest(
                                "TRAM-1",
                                "Skenderija",
                                "Marijin Dvor",
                                LocalDateTime.now().minusHours(2),
                                12));

                mockMvc.perform(get("/api/v1/users/{userId}/travel-history", user.id())
                                .queryParam("page", "0")
                                .queryParam("size", "10")
                                .queryParam("sort", "traveledAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].lineCode").value("TRAM-1"))
                                .andExpect(jsonPath("$.content[0].fromStop").value("Skenderija"))
                                .andExpect(jsonPath("$.number").value(0));
        }

        @Test
        void addTravelHistory_shouldReturnCreatedEntry() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Single Travel",
                                "single.travel@sarajevotransit.ba",
                                "SinglePass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                        "lineCode": "TRAM-3",
                                        "fromStop": "Skenderija",
                                        "toStop": "Bascarsija",
                                        "durationMinutes": 18
                                }
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/travel-history", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.id").isNumber())
                                .andExpect(jsonPath("$.lineCode").value("TRAM-3"));
        }

        @Test
        void addTravelHistory_withInvalidDuration_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Invalid Travel",
                                "invalid.travel@sarajevotransit.ba",
                                "InvalidPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                        "lineCode": "TRAM-3",
                                        "fromStop": "Skenderija",
                                        "toStop": "Bascarsija",
                                        "durationMinutes": 0
                                }
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/travel-history", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void addTravelHistoryBatch_withEmptyPayload_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Empty Batch",
                                "empty.batch@sarajevotransit.ba",
                                "EmptyPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                mockMvc.perform(post("/api/v1/users/{userId}/travel-history/batch", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("[]"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void getTicketPurchases_shouldReturnPaginatedContent() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Purchase User",
                                "purchase.user@sarajevotransit.ba",
                                "PurchasePass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                userService.addTicketPurchase(user.id(), new AddTicketPurchaseRequest(
                                TicketType.MONTHLY,
                                new BigDecimal("53.00"),
                                "CARD",
                                "TXN-PURCHASE-1",
                                "TRAM-3",
                                LocalDateTime.now()));

                mockMvc.perform(get("/api/v1/users/{userId}/ticket-purchases", user.id())
                                .queryParam("page", "0")
                                .queryParam("size", "10")
                                .queryParam("sort", "purchasedAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.content[0].ticketType").value("MONTHLY"));
        }

        @Test
        void addTicketPurchase_withInvalidAmount_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Invalid Purchase",
                                "invalid.purchase@sarajevotransit.ba",
                                "InvalidPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                        "ticketType": "MONTHLY",
                                        "amount": 0.00,
                                        "paymentMethod": "CARD",
                                        "externalTransactionId": "TXN-INVALID-001",
                                        "lineCode": "TRAM-3"
                                }
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/ticket-purchases", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void updateUserProfile_shouldReturnUpdatedFields() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Old Name",
                                "old.name@sarajevotransit.ba",
                                "OldPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                        "fullName": "New Name",
                                        "email": "NEW.NAME@SARAJEVOTRANSIT.BA"
                                }
                                """;

                mockMvc.perform(put("/api/v1/users/{userId}", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fullName").value("New Name"))
                                .andExpect(jsonPath("$.email").value("new.name@sarajevotransit.ba"));
        }

        @Test
        void updateUserProfile_withInvalidPayload_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Invalid Update",
                                "invalid.update@sarajevotransit.ba",
                                "InvalidPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                        "fullName": "",
                                        "email": "not-an-email"
                                }
                                """;

                mockMvc.perform(put("/api/v1/users/{userId}", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void updatePassword_shouldReturnNoContent() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Password User",
                                "password.user@sarajevotransit.ba",
                                "PasswordPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                        "newPassword": "NewStrongPass123"
                                }
                                """;

                mockMvc.perform(put("/api/v1/users/{userId}/password", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isNoContent());
        }

        @Test
        void updatePassword_withInvalidPayload_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Weak Password",
                                "weak.password@sarajevotransit.ba",
                                "WeakPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String payload = """
                                {
                                        "newPassword": "123"
                                }
                                """;

                mockMvc.perform(put("/api/v1/users/{userId}/password", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void getAllUsers_shouldReturnPaginatedContent() throws Exception {
                userService.createUser(new CreateUserRequest(
                                "Amina Hadzic",
                                "amina.hadzic@sarajevotransit.ba",
                                "AminaPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                userService.createUser(new CreateUserRequest(
                                "Tarik Kovac",
                                "tarik.kovac@sarajevotransit.ba",
                                "TarikPass123",
                                LanguageCode.EN,
                                ThemeMode.DARK,
                                NotificationChannel.EMAIL));

                mockMvc.perform(get("/api/v1/users")
                                .queryParam("page", "0")
                                .queryParam("size", "1")
                                .queryParam("sort", "createdAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content.length()").value(1))
                                .andExpect(jsonPath("$.size").value(1))
                                .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void getAllUsers_withNegativePage_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/users")
                                .queryParam("page", "-1")
                                .queryParam("size", "10"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void getSuggestions_withInvalidLimit_shouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Aida Kovac",
                                "aida.kovac@sarajevotransit.ba",
                                "AidaPass123",
                                LanguageCode.BS,
                                ThemeMode.LIGHT,
                                NotificationChannel.PUSH));

                mockMvc.perform(get("/api/v1/users/{userId}/suggestions", user.id())
                                .queryParam("limit", "0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"))
                                .andExpect(jsonPath("$.validationErrors").isArray());
        }

        @Test
        void getSuggestions_shouldReturnRankedLines() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Suggest User",
                                "suggest.user@sarajevotransit.ba",
                                "SuggestPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                userService.addTravelHistory(user.id(), new AddTravelHistoryRequest(
                                "TRAM-3",
                                "Skenderija",
                                "Bascarsija",
                                LocalDateTime.now().minusDays(2),
                                16));
                userService.addTravelHistory(user.id(), new AddTravelHistoryRequest(
                                "TRAM-3",
                                "Skenderija",
                                "Marijin Dvor",
                                LocalDateTime.now().minusDays(1),
                                14));
                userService.addTravelHistory(user.id(), new AddTravelHistoryRequest(
                                "BUS-31E",
                                "Nedzarici",
                                "Dobrinja",
                                LocalDateTime.now(),
                                20));

                mockMvc.perform(get("/api/v1/users/{userId}/suggestions", user.id())
                                .queryParam("limit", "2"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$[0]").value("TRAM-3"));
        }

        @Test
        void getUser_withInvalidPathVariable_shouldReturnBadRequest() throws Exception {
                mockMvc.perform(get("/api/v1/users/0"))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void patchUserProfile_shouldApplyJsonPatchDocument() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Lejla Music",
                                "lejla.patch@sarajevotransit.ba",
                                "LejlaPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String patchPayload = """
                                [
                                  { "op": "replace", "path": "/fullName", "value": "Lejla Updated" },
                                  { "op": "replace", "path": "/email", "value": "LEJLA.UPDATED@SARAJEVOTRANSIT.BA" }
                                ]
                                """;

                mockMvc.perform(patch("/api/v1/users/{userId}", user.id())
                                .contentType("application/json-patch+json")
                                .content(patchPayload))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.fullName").value("Lejla Updated"))
                                .andExpect(jsonPath("$.email").value("lejla.updated@sarajevotransit.ba"));
        }

        @Test
        void patchUserProfile_withInvalidPatchShouldReturnBadRequest() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Amar Kovac",
                                "amar.patch@sarajevotransit.ba",
                                "AmarPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String patchPayload = """
                                [
                                  { "op": "remove", "path": "/fullName" }
                                ]
                                """;

                mockMvc.perform(patch("/api/v1/users/{userId}", user.id())
                                .contentType("application/json-patch+json")
                                .content(patchPayload))
                                .andExpect(status().isBadRequest())
                                .andExpect(jsonPath("$.message").value("Validation failed"));
        }

        @Test
        void addTravelHistoryBatch_shouldCreateMultipleEntries() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Batch User",
                                "batch.user@sarajevotransit.ba",
                                "BatchPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                String batchPayload = """
                                [
                                  {
                                    "lineCode": "TRAM-3",
                                    "fromStop": "Skenderija",
                                    "toStop": "Bascarsija",
                                    "durationMinutes": 18
                                  },
                                  {
                                    "lineCode": "BUS-31E",
                                    "fromStop": "Nedzarici",
                                    "toStop": "Dobrinja",
                                    "durationMinutes": 22
                                  }
                                ]
                                """;

                mockMvc.perform(post("/api/v1/users/{userId}/travel-history/batch", user.id())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(batchPayload))
                                .andExpect(status().isCreated())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].id").isNumber())
                                .andExpect(jsonPath("$[1].id").isNumber());

                mockMvc.perform(get("/api/v1/users/{userId}/travel-history", user.id())
                                .queryParam("page", "0")
                                .queryParam("size", "10")
                                .queryParam("sort", "traveledAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(2));
        }

        @Test
        void ticketPurchaseStats_shouldReturnCustomAggregates() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Stats User",
                                "stats.user@sarajevotransit.ba",
                                "StatsPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                userService.addTicketPurchase(user.id(), new AddTicketPurchaseRequest(
                                TicketType.MONTHLY,
                                new BigDecimal("53.00"),
                                "CARD",
                                "TXN-STATS-001",
                                "TRAM-3",
                                LocalDateTime.now().minusDays(1)));
                userService.addTicketPurchase(user.id(), new AddTicketPurchaseRequest(
                                TicketType.DAILY,
                                new BigDecimal("2.00"),
                                "CARD",
                                "TXN-STATS-002",
                                "BUS-31E",
                                LocalDateTime.now()));

                mockMvc.perform(get("/api/v1/users/{userId}/ticket-purchases/stats", user.id()))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.length()").value(2))
                                .andExpect(jsonPath("$[0].ticketType").value("MONTHLY"))
                                .andExpect(jsonPath("$[0].purchaseCount").value(1));
        }

        @Test
        void ticketPurchaseStats_forMissingUser_shouldReturnNotFound() throws Exception {
                mockMvc.perform(get("/api/v1/users/999/ticket-purchases/stats"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("User with id 999 not found."));
        }

        @Test
        void deleteTravelHistoryEntry_shouldReturnNoContent() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Delete User",
                                "delete.user@sarajevotransit.ba",
                                "DeletePass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                var entry = userService.addTravelHistory(user.id(), new AddTravelHistoryRequest(
                                "TRAM-1",
                                "Skenderija",
                                "Marijin Dvor",
                                LocalDateTime.now(),
                                12));

                mockMvc.perform(delete("/api/v1/users/{userId}/travel-history/{entryId}", user.id(), entry.id()))
                                .andExpect(status().isNoContent());

                mockMvc.perform(get("/api/v1/users/{userId}/travel-history", user.id())
                                .queryParam("page", "0")
                                .queryParam("size", "10")
                                .queryParam("sort", "traveledAt,desc"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.totalElements").value(0));
        }

        @Test
        void deleteTravelHistoryEntry_forMissingEntry_shouldReturnNotFound() throws Exception {
                var user = userService.createUser(new CreateUserRequest(
                                "Missing Entry",
                                "missing.entry@sarajevotransit.ba",
                                "MissingPass123",
                                LanguageCode.BS,
                                ThemeMode.SYSTEM,
                                NotificationChannel.PUSH));

                mockMvc.perform(delete("/api/v1/users/{userId}/travel-history/{entryId}", user.id(), 99999L))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message")
                                                .value("Travel history entry with id 99999 not found for user "
                                                                + user.id() + "."));
        }

        @Test
        void getSummary_forMissingUser_shouldReturnNotFound() throws Exception {
                mockMvc.perform(get("/api/v1/users/999/summary"))
                                .andExpect(status().isNotFound())
                                .andExpect(jsonPath("$.message").value("User with id 999 not found."));
        }
}
