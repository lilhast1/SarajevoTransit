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
}
