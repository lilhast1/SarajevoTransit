package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateNotificationRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.NotificationResponse;
import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.repository.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Spy
    private ModelMapper modelMapper = createModelMapper();

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getAll_returnsAllNotifications() {
        Long userId = 1L;
        Notification first = createNotification(10L, userId, false);
        Notification second = createNotification(11L, userId, true);
        when(notificationRepository.findAll()).thenReturn(List.of(first, second));

        List<NotificationResponse> result = notificationService.getAll();

        assertEquals(2, result.size());
        assertEquals(first.getId(), result.get(0).getId());
        assertEquals(second.getId(), result.get(1).getId());
        verify(notificationRepository).findAll();
    }

    @Test
    void getById_existingId_returnsResponse() {
        Long id = 1L;
        Notification notification = createNotification(id, 2L, false);
        when(notificationRepository.findById(id)).thenReturn(Optional.of(notification));

        NotificationResponse result = notificationService.getById(id);

        assertEquals(id, result.getId());
        assertEquals(notification.getTitle(), result.getTitle());
        verify(notificationRepository).findById(id);
    }

    @Test
    void getById_nonExistingId_throwsException() {
        Long id = 99L;
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> notificationService.getById(id));

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(notificationRepository).findById(id);
    }

    @Test
    void getByUserId_returnsFilteredList() {
        Long userId = 1L;
        Notification notification = createNotification(10L, userId, false);
        when(notificationRepository.findByUserId(userId)).thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        verify(notificationRepository).findByUserId(userId);
    }

    @Test
    void getUnreadByUserId_returnsUnreadOnly() {
        Long userId = 1L;
        Notification unread = createNotification(10L, userId, false);
        when(notificationRepository.findByUserIdAndIsRead(userId, false)).thenReturn(List.of(unread));

        List<NotificationResponse> result = notificationService.getUnreadByUserId(userId);

        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());
        verify(notificationRepository).findByUserIdAndIsRead(userId, false);
    }

    @Test
    void create_savesNotification_returnsResponse() {
        CreateNotificationRequest request = createNotificationRequest();
        Long savedId = 10L;
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> {
            Notification toSave = invocation.getArgument(0);
            toSave.setId(savedId);
            return toSave;
        });

        NotificationResponse result = notificationService.create(request);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        Notification savedEntity = captor.getValue();
        assertEquals(request.getUserId(), savedEntity.getUserId());
        assertFalse(savedEntity.getIsRead());
        assertNotNull(savedEntity.getSentAt());
        assertEquals(savedId, result.getId());
        assertFalse(result.getIsRead());
        assertNotNull(result.getSentAt());
    }

    @Test
    void markAsRead_setsIsReadTrue() {
        Long id = 1L;
        Notification existing = createNotification(id, 2L, false);
        when(notificationRepository.findById(id)).thenReturn(Optional.of(existing));
        when(notificationRepository.save(any(Notification.class))).thenAnswer(invocation -> invocation.getArgument(0));

        NotificationResponse result = notificationService.markAsRead(id);

        assertTrue(result.getIsRead());
        verify(notificationRepository).findById(id);
        verify(notificationRepository).save(existing);
    }

    @Test
    void markAsRead_nonExistingId_throwsException() {
        Long id = 99L;
        when(notificationRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> notificationService.markAsRead(id));

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(notificationRepository).findById(id);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void delete_existingId_deletesSuccessfully() {
        Long id = 1L;
        when(notificationRepository.existsById(id)).thenReturn(true);

        notificationService.delete(id);

        verify(notificationRepository).existsById(id);
        verify(notificationRepository).deleteById(id);
    }

    @Test
    void delete_nonExistingId_throwsException() {
        Long id = 99L;
        when(notificationRepository.existsById(id)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> notificationService.delete(id));

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(notificationRepository).existsById(id);
        verify(notificationRepository, never()).deleteById(id);
    }

    private static Notification createNotification(Long id, Long userId, boolean isRead) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setUserId(userId);
        notification.setUserFullName("Test User");
        notification.setUserEmail("user@example.com");
        notification.setLineId(101L);
        notification.setLineCode("L1");
        notification.setLineName("Line 1");
        notification.setType(NotificationType.GENERAL);
        notification.setTitle("Title");
        notification.setContent("Content");
        notification.setIsRead(isRead);
        notification.setSentAt(LocalDateTime.now().minusMinutes(5));
        return notification;
    }

    private static CreateNotificationRequest createNotificationRequest() {
        CreateNotificationRequest request = new CreateNotificationRequest();
        request.setUserId(1L);
        request.setUserFullName("Test User");
        request.setUserEmail("user@example.com");
        request.setLineId(101L);
        request.setLineCode("L1");
        request.setLineName("Line 1");
        request.setType(NotificationType.GENERAL);
        request.setTitle("New Notification");
        request.setContent("Body");
        return request;
    }

    private static ModelMapper createModelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }
}
