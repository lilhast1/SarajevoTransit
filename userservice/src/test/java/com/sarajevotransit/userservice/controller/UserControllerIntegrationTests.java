package com.sarajevotransit.userservice.controller;

import com.sarajevotransit.userservice.dto.AddTravelHistoryRequest;
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

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

        mockMvc.perform(get("/api/v1/users/{userId}/travel-history", user.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineCode").value("TRAM-1"))
                .andExpect(jsonPath("$[0].fromStop").value("Skenderija"));
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
}
