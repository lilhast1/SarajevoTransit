package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SubscriptionController.class)
class SubscriptionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SubscriptionService subscriptionService;

    @Test
    void getAll_returns200() throws Exception {
        SubscriptionResponse response = subscriptionResponse(UUID.randomUUID(), UUID.randomUUID(), true);
        when(subscriptionService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.getId().toString()));
    }

    @Test
    void getById_existingId_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionResponse response = subscriptionResponse(id, UUID.randomUUID(), true);
        when(subscriptionService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/subscriptions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        String message = "Subscription not found: " + id;
        when(subscriptionService.getById(id)).thenThrow(new NoSuchElementException(message));

        mockMvc.perform(get("/subscriptions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(message));
    }

    @Test
    void getByUserId_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        SubscriptionResponse response = subscriptionResponse(UUID.randomUUID(), userId, true);
        when(subscriptionService.getByUserId(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId.toString()));
    }

    @Test
    void getActive_returns200() throws Exception {
        UUID userId = UUID.randomUUID();
        SubscriptionResponse response = subscriptionResponse(UUID.randomUUID(), userId, true);
        when(subscriptionService.getActiveByUserId(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/user/{userId}/active", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void getByLineId_returns200() throws Exception {
        UUID lineId = UUID.randomUUID();
        SubscriptionResponse response = subscriptionResponse(UUID.randomUUID(), UUID.randomUUID(), true);
        response.setLineId(lineId);
        when(subscriptionService.getByLineId(lineId)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/line/{lineId}", lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].lineId").value(lineId.toString()));
    }

    @Test
    void search_byEmail_returns200() throws Exception {
        String email = "user@example.com";
        SubscriptionResponse response = subscriptionResponse(UUID.randomUUID(), UUID.randomUUID(), true);
        response.setUserEmail(email);
        when(subscriptionService.searchByEmail(email)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/search").param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value(email));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CreateSubscriptionRequest request = validCreateRequest();
        SubscriptionResponse response = subscriptionResponse(UUID.randomUUID(), request.getUserId(), true);
        when(subscriptionService.create(any(CreateSubscriptionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId().toString()));
    }

    @Test
    void create_missingLineId_returns400() throws Exception {
        CreateSubscriptionRequest request = validCreateRequest();
        request.setLineId(null);

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.lineId").exists());
    }

    @Test
    void create_missingIntervals_returns400() throws Exception {
        CreateSubscriptionRequest request = validCreateRequest();
        request.setStartInterval(null);
        request.setEndInterval(null);

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.startInterval").exists())
                .andExpect(jsonPath("$.endInterval").exists());
    }

    @Test
    void deactivate_returns200() throws Exception {
        UUID id = UUID.randomUUID();
        SubscriptionResponse response = subscriptionResponse(id, UUID.randomUUID(), false);
        when(subscriptionService.deactivate(id)).thenReturn(response);

        mockMvc.perform(patch("/subscriptions/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void delete_existingId_returns204() throws Exception {
        UUID id = UUID.randomUUID();

        mockMvc.perform(delete("/subscriptions/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        UUID id = UUID.randomUUID();
        String message = "Subscription not found: " + id;
        doThrow(new NoSuchElementException(message)).when(subscriptionService).delete(id);

        mockMvc.perform(delete("/subscriptions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value(message));
    }

    private static SubscriptionResponse subscriptionResponse(UUID id, UUID userId, boolean isActive) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(id);
        response.setUserId(userId);
        response.setUserFullName("Test User");
        response.setUserEmail("user@example.com");
        response.setLineId(UUID.randomUUID());
        response.setLineCode("L1");
        response.setLineName("Line 1");
        response.setStartInterval(LocalTime.of(8, 0));
        response.setEndInterval(LocalTime.of(10, 0));
        response.setDaysOfWeek("MON-FRI");
        response.setIsActive(isActive);
        response.setCreatedAt(LocalDateTime.now().minusDays(1));
        return response;
    }

    private static CreateSubscriptionRequest validCreateRequest() {
        CreateSubscriptionRequest request = new CreateSubscriptionRequest();
        request.setUserId(UUID.randomUUID());
        request.setUserFullName("Test User");
        request.setUserEmail("user@example.com");
        request.setLineId(UUID.randomUUID());
        request.setLineCode("L1");
        request.setLineName("Line 1");
        request.setStartInterval(LocalTime.of(8, 0));
        request.setEndInterval(LocalTime.of(10, 0));
        request.setDaysOfWeek("MON-FRI");
        return request;
    }
}
