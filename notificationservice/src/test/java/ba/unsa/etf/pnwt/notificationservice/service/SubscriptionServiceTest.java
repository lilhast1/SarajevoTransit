package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.PagedResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;
import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Spy
    private ModelMapper modelMapper = createModelMapper();

    @InjectMocks
    private SubscriptionService subscriptionService;

    @Test
    void getAll_returnsPagedSubscriptions() {
        Subscription first = createSubscription(10L, 1L, 101L, true);
        Subscription second = createSubscription(11L, 2L, 102L, true);
        Pageable pageable = PageRequest.of(0, 20);
        when(subscriptionRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(first, second)));

        PagedResponse<SubscriptionResponse> result = subscriptionService.getAll(pageable);

        assertEquals(2, result.getContent().size());
        assertEquals(first.getId(), result.getContent().get(0).getId());
        assertEquals(second.getId(), result.getContent().get(1).getId());
        assertEquals(2L, result.getTotalElements());
        verify(subscriptionRepository).findAll(pageable);
    }

    @Test
    void getById_existingId_returnsResponse() {
        Long id = 1L;
        Subscription subscription = createSubscription(id, 2L, 101L, true);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        SubscriptionResponse result = subscriptionService.getById(id);

        assertEquals(id, result.getId());
        assertEquals(subscription.getUserEmail(), result.getUserEmail());
        verify(subscriptionRepository).findById(id);
    }

    @Test
    void getById_nonExistingId_throwsException() {
        Long id = 99L;
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        NotFoundException ex = assertThrows(NotFoundException.class, () -> subscriptionService.getById(id));

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(subscriptionRepository).findById(id);
    }

    @Test
    void getByUserId_returnsSubscriptions() {
        Long userId = 1L;
        Subscription subscription = createSubscription(10L, userId, 101L, true);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.getByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        verify(subscriptionRepository).findByUserId(userId);
    }

    @Test
    void getByLineId_returnsPagedSubscriptions() {
        Long lineId = 101L;
        Subscription subscription = createSubscription(10L, 1L, lineId, true);
        Pageable pageable = PageRequest.of(0, 20);
        when(subscriptionRepository.findByLineId(lineId, pageable)).thenReturn(new PageImpl<>(List.of(subscription)));

        PagedResponse<SubscriptionResponse> result = subscriptionService.getByLineId(lineId, pageable);

        assertEquals(1, result.getContent().size());
        assertEquals(lineId, result.getContent().get(0).getLineId());
        verify(subscriptionRepository).findByLineId(lineId, pageable);
    }

    @Test
    void getActiveByUserId_returnsOnlyActive() {
        Long userId = 1L;
        Subscription activeSubscription = createSubscription(10L, userId, 101L, true);
        when(subscriptionRepository.findByUserIdAndIsActive(userId, true)).thenReturn(List.of(activeSubscription));

        List<SubscriptionResponse> result = subscriptionService.getActiveByUserId(userId);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(subscriptionRepository).findByUserIdAndIsActive(userId, true);
    }

    @Test
    void searchByName_returnsMatches() {
        String name = "Ali";
        Subscription subscription = createSubscription(10L, 1L, 101L, true);
        subscription.setUserFullName("Ali Test");
        when(subscriptionRepository.findByUserFullNameContainingIgnoreCase(name)).thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.searchByName(name);

        assertEquals(1, result.size());
        assertEquals("Ali Test", result.get(0).getUserFullName());
        verify(subscriptionRepository).findByUserFullNameContainingIgnoreCase(name);
    }

    @Test
    void searchByEmail_returnsMatches() {
        String email = "user@example.com";
        Subscription subscription = createSubscription(10L, 1L, 101L, true);
        subscription.setUserEmail(email);
        when(subscriptionRepository.findByUserEmailIgnoreCase(email)).thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.searchByEmail(email);

        assertEquals(1, result.size());
        assertEquals(email, result.get(0).getUserEmail());
        verify(subscriptionRepository).findByUserEmailIgnoreCase(email);
    }

    @Test
    void countActiveByLineId_returnsCorrectCount() {
        Long lineId = 101L;
        when(subscriptionRepository.countActiveByLineId(lineId)).thenReturn(5L);

        long count = subscriptionService.countActiveByLineId(lineId);

        assertEquals(5L, count);
        verify(subscriptionRepository).countActiveByLineId(lineId);
    }

    @Test
    void getActiveForLineAtTime_returnsMatchingSubscriptions() {
        Long lineId = 101L;
        LocalTime targetTime = LocalTime.of(9, 0);
        String dayAbbr = "MON";
        Subscription subscription = createSubscription(10L, 1L, lineId, true);
        when(subscriptionRepository.findActiveForLineAtTime(lineId, targetTime, dayAbbr))
                .thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.getActiveForLineAtTime(lineId, targetTime, dayAbbr);

        assertEquals(1, result.size());
        assertEquals(lineId, result.get(0).getLineId());
        verify(subscriptionRepository).findActiveForLineAtTime(lineId, targetTime, dayAbbr);
    }

    @Test
    void create_savesSubscription_isActiveTrue() {
        CreateSubscriptionRequest request = createSubscriptionRequest();
        Long id = 10L;
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription entity = invocation.getArgument(0);
            entity.setId(id);
            return entity;
        });

        SubscriptionResponse result = subscriptionService.create(request);

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionRepository).save(captor.capture());
        Subscription savedEntity = captor.getValue();
        assertEquals(request.getUserId(), savedEntity.getUserId());
        assertTrue(savedEntity.getIsActive());
        assertEquals(id, result.getId());
        assertTrue(result.getIsActive());
    }

    @Test
    void deactivate_setsIsActiveFalse() {
        Long id = 1L;
        Subscription existing = createSubscription(id, 2L, 101L, true);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse result = subscriptionService.deactivate(id);

        assertFalse(result.getIsActive());
        verify(subscriptionRepository).findById(id);
        verify(subscriptionRepository).save(existing);
    }

    @Test
    void delete_existingId_deletesSuccessfully() {
        Long id = 1L;
        when(subscriptionRepository.existsById(id)).thenReturn(true);

        subscriptionService.delete(id);

        verify(subscriptionRepository).existsById(id);
        verify(subscriptionRepository).deleteById(id);
    }

    @Test
    void delete_nonExistingId_throwsException() {
        Long id = 99L;
        when(subscriptionRepository.existsById(id)).thenReturn(false);

        NotFoundException ex = assertThrows(NotFoundException.class, () -> subscriptionService.delete(id));

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(subscriptionRepository).existsById(id);
        verify(subscriptionRepository, never()).deleteById(id);
    }

    private static Subscription createSubscription(Long id, Long userId, Long lineId, boolean isActive) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setUserId(userId);
        subscription.setUserFullName("Test User");
        subscription.setUserEmail("user@example.com");
        subscription.setLineId(lineId);
        subscription.setLineCode("L1");
        subscription.setLineName("Line 1");
        subscription.setStartInterval(LocalTime.of(8, 0));
        subscription.setEndInterval(LocalTime.of(12, 0));
        subscription.setDaysOfWeek("MON-FRI");
        subscription.setIsActive(isActive);
        subscription.setCreatedAt(LocalDateTime.now().minusDays(1));
        return subscription;
    }

    private static CreateSubscriptionRequest createSubscriptionRequest() {
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

    private static ModelMapper createModelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }
}
