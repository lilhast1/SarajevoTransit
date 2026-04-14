package ba.unsa.etf.pnwt.notificationservice.controller;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateNotificationRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.NotificationResponse;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
    void getAll_returns200WithList() throws Exception {
        NotificationResponse response = notificationResponse(1L, 2L, false);
        when(notificationService.getAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(response.getId()));
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
    void getByUserId_returns200() throws Exception {
        Long userId = 1L;
        NotificationResponse response = notificationResponse(10L, userId, false);
        when(notificationService.getByUserId(userId)).thenReturn(List.of(response));

        mockMvc.perform(get("/notifications/user/{userId}", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].userId").value(userId));
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
