package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.*;
import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;
import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import ba.unsa.etf.pnwt.notificationservice.model.NotificationType;
import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
import ba.unsa.etf.pnwt.notificationservice.repository.NotificationRepository;
import ba.unsa.etf.pnwt.notificationservice.repository.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.modelmapper.ModelMapper;
import org.modelmapper.convention.MatchingStrategies;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Spy
    private ModelMapper modelMapper = createModelMapper();

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void getAll_returnsPagedNotifications() {
        Long userId = 1L;
        Notification first = createNotification(10L, userId, false);
        Notification second = createNotification(11L, userId, true);
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(first, second)));

        PagedResponse<NotificationResponse> result = notificationService.getAll(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(first.getId(), result.getContent().get(0).getId());
        assertEquals(second.getId(), result.getContent().get(1).getId());
        assertEquals(2L, result.getTotalElements());
        verify(notificationRepository).findAll(pageable);
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
    void getByUserId_returnsPagedList() {
        Long userId = 1L;
        Notification notification = createNotification(10L, userId, false);
        Pageable pageable = PageRequest.of(0, 20);
        when(notificationRepository.findByUserId(userId, pageable)).thenReturn(new PageImpl<>(List.of(notification)));

        PagedResponse<NotificationResponse> result = notificationService.getByUserId(userId, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(userId, result.getContent().get(0).getUserId());
        verify(notificationRepository).findByUserId(userId, pageable);
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
    void countUnreadByUserId_returnsCorrectCount() {
        Long userId = 1L;
        when(notificationRepository.countUnreadByUserId(userId)).thenReturn(3L);

        long count = notificationService.countUnreadByUserId(userId);

        assertEquals(3L, count);
        verify(notificationRepository).countUnreadByUserId(userId);
    }

    @Test
    void getByUserIdAndDateRange_returnsNotificationsInRange() {
        Long userId = 1L;
        LocalDateTime from = LocalDateTime.now().minusDays(7);
        LocalDateTime to = LocalDateTime.now();
        Notification notification = createNotification(10L, userId, false);
        when(notificationRepository.findByUserIdAndSentAtBetween(userId, from, to)).thenReturn(List.of(notification));

        List<NotificationResponse> result = notificationService.getByUserIdAndDateRange(userId, from, to);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        verify(notificationRepository).findByUserIdAndSentAtBetween(userId, from, to);
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
    void createBatch_savesAllNotifications_returnsResponses() {
        BatchCreateNotificationRequest batchRequest = new BatchCreateNotificationRequest();
        batchRequest.setNotifications(List.of(createNotificationRequest(), createNotificationRequest()));
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<Notification> list = invocation.getArgument(0);
            for (int i = 0; i < list.size(); i++) list.get(i).setId((long) (i + 1));
            return list;
        });

        List<NotificationResponse> result = notificationService.createBatch(batchRequest);

        assertEquals(2, result.size());
        result.forEach(r -> {
            assertFalse(r.getIsRead());
            assertNotNull(r.getSentAt());
        });
        verify(notificationRepository).saveAll(argThat((Iterable<Notification> it) -> ((List<?>) it).size() == 2));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void broadcast_withActiveSubscribers_createsNotificationsForAll() {
        Long lineId = 101L;
        Subscription sub1 = createSubscription(1L, 10L, lineId);
        Subscription sub2 = createSubscription(2L, 11L, lineId);
        when(subscriptionRepository.findByLineIdAndIsActive(lineId, true)).thenReturn(List.of(sub1, sub2));
        when(notificationRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        BroadcastNotificationRequest request = new BroadcastNotificationRequest();
        request.setLineId(lineId);
        request.setType(NotificationType.DELAY);
        request.setTitle("Line delayed");
        request.setContent("Expect delays on line L1");

        BroadcastNotificationResponse result = notificationService.broadcast(request);

        assertEquals(2, result.getNotificationsCreated());
        assertEquals(lineId, result.getLineId());
        verify(notificationRepository).saveAll(argThat((Iterable<Notification> it) -> ((List<?>) it).size() == 2));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void broadcast_withNoSubscribers_returnsZeroCount() {
        Long lineId = 999L;
        when(subscriptionRepository.findByLineIdAndIsActive(lineId, true)).thenReturn(List.of());

        BroadcastNotificationRequest request = new BroadcastNotificationRequest();
        request.setLineId(lineId);
        request.setType(NotificationType.DELAY);
        request.setTitle("Delayed");
        request.setContent("No service");

        BroadcastNotificationResponse result = notificationService.broadcast(request);

        assertEquals(0, result.getNotificationsCreated());
        verify(notificationRepository, never()).saveAll(any());
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

    private static Subscription createSubscription(Long id, Long userId, Long lineId) {
        Subscription sub = new Subscription();
        sub.setId(id);
        sub.setUserId(userId);
        sub.setUserFullName("Test User");
        sub.setUserEmail("user@example.com");
        sub.setLineId(lineId);
        sub.setLineCode("L1");
        sub.setLineName("Line 1");
        sub.setStartInterval(LocalTime.of(8, 0));
        sub.setEndInterval(LocalTime.of(18, 0));
        sub.setDaysOfWeek("MON,TUE,WED,THU,FRI");
        sub.setIsActive(true);
        return sub;
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
