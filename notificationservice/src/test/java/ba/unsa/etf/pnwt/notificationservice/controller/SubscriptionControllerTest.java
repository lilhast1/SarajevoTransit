package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.PagedResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.UpdateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;
import ba.unsa.etf.pnwt.notificationservice.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
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
    void getAll_returns200WithPagedResponse() throws Exception {
        SubscriptionResponse response = subscriptionResponse(1L, 2L, true);
        PagedResponse<SubscriptionResponse> paged = new PagedResponse<>(List.of(response), 0, 20, 1L, 1, true);
        when(subscriptionService.getAll(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/subscriptions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(response.getId()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getById_existingId_returns200() throws Exception {
        Long id = 1L;
        SubscriptionResponse response = subscriptionResponse(id, 2L, true);
        when(subscriptionService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/subscriptions/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        Long id = 99L;
        String message = "Subscription not found: " + id;
        when(subscriptionService.getById(id)).thenThrow(new NotFoundException(message));

        mockMvc.perform(get("/subscriptions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void getByUserId_returns200() throws Exception {
        Long userId = 1L;
        SubscriptionResponse response = subscriptionResponse(10L, userId, true);
        when(subscriptionService.getByUserId(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void getActive_returns200() throws Exception {
        Long userId = 1L;
        SubscriptionResponse response = subscriptionResponse(10L, userId, true);
        when(subscriptionService.getActiveByUserId(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/user/{userId}/active", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isActive").value(true));
    }

    @Test
    void getByLineId_returns200WithPagedResponse() throws Exception {
        Long lineId = 101L;
        SubscriptionResponse response = subscriptionResponse(10L, 1L, true);
        response.setLineId(lineId);
        PagedResponse<SubscriptionResponse> paged = new PagedResponse<>(List.of(response), 0, 20, 1L, 1, true);
        when(subscriptionService.getByLineId(eq(lineId), any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/subscriptions/line/{lineId}", lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].lineId").value(lineId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void countActiveByLine_returns200WithCount() throws Exception {
        Long lineId = 101L;
        when(subscriptionService.countActiveByLineId(lineId)).thenReturn(7L);

        mockMvc.perform(get("/subscriptions/line/{lineId}/active/count", lineId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(7));
    }

    @Test
    void getActiveAtTime_returns200() throws Exception {
        Long lineId = 101L;
        SubscriptionResponse response = subscriptionResponse(10L, 1L, true);
        when(subscriptionService.getActiveForLineAtTime(eq(lineId), any(LocalTime.class), eq("MON")))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/line/{lineId}/active-at", lineId)
                        .param("time", "09:00:00")
                        .param("day", "MON"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10L));
    }

    @Test
    void search_byEmail_returns200() throws Exception {
        String email = "user@example.com";
        SubscriptionResponse response = subscriptionResponse(10L, 1L, true);
        response.setUserEmail(email);
        when(subscriptionService.searchByEmail(email)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/search").param("email", email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userEmail").value(email));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CreateSubscriptionRequest request = validCreateRequest();
        SubscriptionResponse response = subscriptionResponse(10L, request.getUserId(), true);
        when(subscriptionService.create(any(CreateSubscriptionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId()));
    }

    @Test
    void create_missingLineId_returns400() throws Exception {
        CreateSubscriptionRequest request = validCreateRequest();
        request.setLineId(null);

        mockMvc.perform(post("/subscriptions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.lineId").exists());
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
                .andExpect(jsonPath("$.validationErrors.startInterval").exists())
                .andExpect(jsonPath("$.validationErrors.endInterval").exists());
    }

    @Test
    void deactivate_notFound_returns404() throws Exception {
        Long id = 99L;
        String message = "Subscription not found: " + id;
        doThrow(new NotFoundException(message)).when(subscriptionService).deactivate(id);

        mockMvc.perform(patch("/subscriptions/{id}/deactivate", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void activate_returns200() throws Exception {
        Long id = 1L;
        SubscriptionResponse response = subscriptionResponse(id, 2L, true);
        when(subscriptionService.activate(id)).thenReturn(response);

        mockMvc.perform(patch("/subscriptions/{id}/activate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(true));
    }

    @Test
    void activate_notFound_returns404() throws Exception {
        Long id = 99L;
        String message = "Subscription not found: " + id;
        doThrow(new NotFoundException(message)).when(subscriptionService).activate(id);

        mockMvc.perform(patch("/subscriptions/{id}/activate", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void update_validRequest_returns200() throws Exception {
        Long id = 1L;
        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest();
        request.setLineCode("L2");
        SubscriptionResponse response = subscriptionResponse(id, 2L, true);
        response.setLineCode("L2");
        when(subscriptionService.update(eq(id), any(UpdateSubscriptionRequest.class))).thenReturn(response);

        mockMvc.perform(patch("/subscriptions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lineCode").value("L2"));
    }

    @Test
    void update_notFound_returns404() throws Exception {
        Long id = 99L;
        String message = "Subscription not found: " + id;
        UpdateSubscriptionRequest request = new UpdateSubscriptionRequest();
        request.setLineCode("L2");
        doThrow(new NotFoundException(message)).when(subscriptionService).update(eq(id), any(UpdateSubscriptionRequest.class));

        mockMvc.perform(patch("/subscriptions/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void search_byName_returns200() throws Exception {
        String name = "Ali";
        SubscriptionResponse response = subscriptionResponse(10L, 1L, true);
        response.setUserFullName("Ali Aljaljak");
        when(subscriptionService.searchByName(name)).thenReturn(List.of(response));

        mockMvc.perform(get("/subscriptions/search").param("name", name))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userFullName").value("Ali Aljaljak"));
    }

    @Test
    void deactivate_returns200() throws Exception {
        Long id = 1L;
        SubscriptionResponse response = subscriptionResponse(id, 2L, false);
        when(subscriptionService.deactivate(id)).thenReturn(response);

        mockMvc.perform(patch("/subscriptions/{id}/deactivate", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isActive").value(false));
    }

    @Test
    void delete_existingId_returns204() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/subscriptions/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        Long id = 99L;
        String message = "Subscription not found: " + id;
        doThrow(new NotFoundException(message)).when(subscriptionService).delete(id);

        mockMvc.perform(delete("/subscriptions/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    private static SubscriptionResponse subscriptionResponse(Long id, Long userId, boolean isActive) {
        SubscriptionResponse response = new SubscriptionResponse();
        response.setId(id);
        response.setUserId(userId);
        response.setUserFullName("Test User");
        response.setUserEmail("user@example.com");
        response.setLineId(101L);
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
        request.setUserId(1L);
        request.setUserFullName("Test User");
        request.setUserEmail("user@example.com");
        request.setLineId(101L);
        request.setLineCode("L1");
        request.setLineName("Line 1");
        request.setStartInterval(LocalTime.of(8, 0));
        request.setEndInterval(LocalTime.of(10, 0));
        request.setDaysOfWeek("MON-FRI");
        return request;
    }
}
