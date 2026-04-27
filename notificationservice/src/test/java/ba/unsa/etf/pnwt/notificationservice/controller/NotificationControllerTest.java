package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.*;
import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private NotificationService notificationService;

    @Test
    void getAll_returns200WithPagedResponse() throws Exception {
        NotificationResponse response = notificationResponse(1L, 2L, false);
        PagedResponse<NotificationResponse> paged = new PagedResponse<>(List.of(response), 0, 20, 1L, 1, true);
        when(notificationService.getAll(any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(response.getId()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.page").value(0));
    }

    @Test
    void getById_existingId_returns200() throws Exception {
        Long id = 1L;
        NotificationResponse response = notificationResponse(id, 2L, false);
        when(notificationService.getById(id)).thenReturn(response);

        mockMvc.perform(get("/notifications/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    void getById_notFound_returns404() throws Exception {
        Long id = 99L;
        String message = "Notification not found: " + id;
        when(notificationService.getById(id)).thenThrow(new NotFoundException(message));

        mockMvc.perform(get("/notifications/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    @Test
    void getByUserId_returns200WithPagedResponse() throws Exception {
        Long userId = 1L;
        NotificationResponse response = notificationResponse(10L, userId, false);
        PagedResponse<NotificationResponse> paged = new PagedResponse<>(List.of(response), 0, 20, 1L, 1, true);
        when(notificationService.getByUserId(eq(userId), any(Pageable.class))).thenReturn(paged);

        mockMvc.perform(get("/notifications/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].userId").value(userId))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void getUnread_returns200() throws Exception {
        Long userId = 1L;
        NotificationResponse response = notificationResponse(10L, userId, false);
        when(notificationService.getUnreadByUserId(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/notifications/user/{userId}/unread", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].isRead").value(false));
    }

    @Test
    void countUnread_returns200WithCount() throws Exception {
        Long userId = 1L;
        when(notificationService.countUnreadByUserId(userId)).thenReturn(3L);

        mockMvc.perform(get("/notifications/user/{userId}/unread/count", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3));
    }

    @Test
    void getByDateRange_returns200() throws Exception {
        Long userId = 1L;
        NotificationResponse response = notificationResponse(10L, userId, false);
        when(notificationService.getByUserIdAndDateRange(eq(userId), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(response));

        mockMvc.perform(get("/notifications/user/{userId}/range", userId)
                        .param("from", "2025-01-01T00:00:00")
                        .param("to", "2025-12-31T23:59:59"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId));
    }

    @Test
    void create_validRequest_returns201() throws Exception {
        CreateNotificationRequest request = validCreateRequest();
        NotificationResponse response = notificationResponse(10L, request.getUserId(), false);
        when(notificationService.create(any(CreateNotificationRequest.class))).thenReturn(response);

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(response.getId()));
    }

    @Test
    void create_missingRequiredFields_returns400() throws Exception {
        CreateNotificationRequest request = new CreateNotificationRequest();

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.userId").exists())
                .andExpect(jsonPath("$.validationErrors.type").exists())
                .andExpect(jsonPath("$.validationErrors.title").exists())
                .andExpect(jsonPath("$.validationErrors.content").exists());
    }

    @Test
    void create_invalidEmail_returns400() throws Exception {
        CreateNotificationRequest request = validCreateRequest();
        request.setUserEmail("invalid-email");

        mockMvc.perform(post("/notifications")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.validationErrors.userEmail").exists());
    }

    @Test
    void createBatch_validRequest_returns201() throws Exception {
        BatchCreateNotificationRequest batchRequest = new BatchCreateNotificationRequest();
        batchRequest.setNotifications(List.of(validCreateRequest(), validCreateRequest()));
        List<NotificationResponse> responses = List.of(
                notificationResponse(1L, 1L, false),
                notificationResponse(2L, 1L, false)
        );
        when(notificationService.createBatch(any(BatchCreateNotificationRequest.class))).thenReturn(responses);

        mockMvc.perform(post("/notifications/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[1].id").value(2L));
    }

    @Test
    void createBatch_emptyList_returns400() throws Exception {
        BatchCreateNotificationRequest batchRequest = new BatchCreateNotificationRequest();
        batchRequest.setNotifications(List.of());

        mockMvc.perform(post("/notifications/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(batchRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void broadcast_validRequest_returns201() throws Exception {
        BroadcastNotificationRequest request = new BroadcastNotificationRequest();
        request.setLineId(101L);
        request.setType(NotificationType.DELAY);
        request.setTitle("Delay");
        request.setContent("Bus delayed");

        BroadcastNotificationResponse broadcastResponse = new BroadcastNotificationResponse(3, 101L, "L1", "Line 1");
        when(notificationService.broadcast(any(BroadcastNotificationRequest.class))).thenReturn(broadcastResponse);

        mockMvc.perform(post("/notifications/broadcast")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.notificationsCreated").value(3))
                .andExpect(jsonPath("$.lineId").value(101L));
    }

    @Test
    void markAsRead_returns200() throws Exception {
        Long id = 1L;
        NotificationResponse response = notificationResponse(id, 2L, true);
        when(notificationService.markAsRead(id)).thenReturn(response);

        mockMvc.perform(patch("/notifications/{id}/read", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isRead").value(true));
    }

    @Test
    void delete_existingId_returns204() throws Exception {
        Long id = 1L;

        mockMvc.perform(delete("/notifications/{id}", id))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_notFound_returns404() throws Exception {
        Long id = 99L;
        String message = "Notification not found: " + id;
        doThrow(new NotFoundException(message)).when(notificationService).delete(id);

        mockMvc.perform(delete("/notifications/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(message));
    }

    private static NotificationResponse notificationResponse(Long id, Long userId, boolean isRead) {
        NotificationResponse response = new NotificationResponse();
        response.setId(id);
        response.setUserId(userId);
        response.setUserFullName("Test User");
        response.setUserEmail("user@example.com");
        response.setLineId(101L);
        response.setLineCode("L1");
        response.setLineName("Line 1");
        response.setType(NotificationType.GENERAL);
        response.setTitle("Title");
        response.setContent("Content");
        response.setIsRead(isRead);
        response.setSentAt(LocalDateTime.now().minusMinutes(1));
        return response;
    }

    private static CreateNotificationRequest validCreateRequest() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(1L);
        request.setUserFullName("Test User");
        request.setUserEmail("user@example.com");
        request.setLineId(101L);
        request.setLineCode("L1");
        request.setLineName("Line 1");
        request.setType(NotificationType.GENERAL);
        request.setTitle("Title");
        request.setContent("Content");
        return request;
    }
}
