package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
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

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

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
    void getAll_returnsAllSubscriptions() {
        Subscription first = createSubscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
        Subscription second = createSubscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
        when(subscriptionRepository.findAll()).thenReturn(List.of(first, second));

        List<SubscriptionResponse> result = subscriptionService.getAll();

        assertEquals(2, result.size());
        assertEquals(first.getId(), result.get(0).getId());
        assertEquals(second.getId(), result.get(1).getId());
        verify(subscriptionRepository).findAll();
    }

    @Test
    void getById_existingId_returnsResponse() {
        UUID id = UUID.randomUUID();
        Subscription subscription = createSubscription(id, UUID.randomUUID(), UUID.randomUUID(), true);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(subscription));

        SubscriptionResponse result = subscriptionService.getById(id);

        assertEquals(id, result.getId());
        assertEquals(subscription.getUserEmail(), result.getUserEmail());
        verify(subscriptionRepository).findById(id);
    }

    @Test
    void getById_nonExistingId_throwsException() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.findById(id)).thenReturn(Optional.empty());

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> subscriptionService.getById(id));

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(subscriptionRepository).findById(id);
    }

    @Test
    void getByUserId_returnsSubscriptions() {
        UUID userId = UUID.randomUUID();
        Subscription subscription = createSubscription(UUID.randomUUID(), userId, UUID.randomUUID(), true);
        when(subscriptionRepository.findByUserId(userId)).thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.getByUserId(userId);

        assertEquals(1, result.size());
        assertEquals(userId, result.get(0).getUserId());
        verify(subscriptionRepository).findByUserId(userId);
    }

    @Test
    void getByLineId_returnsSubscriptions() {
        UUID lineId = UUID.randomUUID();
        Subscription subscription = createSubscription(UUID.randomUUID(), UUID.randomUUID(), lineId, true);
        when(subscriptionRepository.findByLineId(lineId)).thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.getByLineId(lineId);

        assertEquals(1, result.size());
        assertEquals(lineId, result.get(0).getLineId());
        verify(subscriptionRepository).findByLineId(lineId);
    }

    @Test
    void getActiveByUserId_returnsOnlyActive() {
        UUID userId = UUID.randomUUID();
        Subscription activeSubscription = createSubscription(UUID.randomUUID(), userId, UUID.randomUUID(), true);
        when(subscriptionRepository.findByUserIdAndIsActive(userId, true)).thenReturn(List.of(activeSubscription));

        List<SubscriptionResponse> result = subscriptionService.getActiveByUserId(userId);

        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(subscriptionRepository).findByUserIdAndIsActive(userId, true);
    }

    @Test
    void searchByName_returnsMatches() {
        String name = "Ali";
        Subscription subscription = createSubscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
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
        Subscription subscription = createSubscription(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true);
        subscription.setUserEmail(email);
        when(subscriptionRepository.findByUserEmailIgnoreCase(email)).thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = subscriptionService.searchByEmail(email);

        assertEquals(1, result.size());
        assertEquals(email, result.get(0).getUserEmail());
        verify(subscriptionRepository).findByUserEmailIgnoreCase(email);
    }

    @Test
    void create_savesSubscription_isActiveTrue() {
        CreateSubscriptionRequest request = createSubscriptionRequest();
        UUID id = UUID.randomUUID();
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
        UUID id = UUID.randomUUID();
        Subscription existing = createSubscription(id, UUID.randomUUID(), UUID.randomUUID(), true);
        when(subscriptionRepository.findById(id)).thenReturn(Optional.of(existing));
        when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SubscriptionResponse result = subscriptionService.deactivate(id);

        assertFalse(result.getIsActive());
        verify(subscriptionRepository).findById(id);
        verify(subscriptionRepository).save(existing);
    }

    @Test
    void delete_existingId_deletesSuccessfully() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.existsById(id)).thenReturn(true);

        subscriptionService.delete(id);

        verify(subscriptionRepository).existsById(id);
        verify(subscriptionRepository).deleteById(id);
    }

    @Test
    void delete_nonExistingId_throwsException() {
        UUID id = UUID.randomUUID();
        when(subscriptionRepository.existsById(id)).thenReturn(false);

        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> subscriptionService.delete(id));

        assertTrue(ex.getMessage().contains(id.toString()));
        verify(subscriptionRepository).existsById(id);
        verify(subscriptionRepository, never()).deleteById(id);
    }

    private static Subscription createSubscription(UUID id, UUID userId, UUID lineId, boolean isActive) {
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

    private static ModelMapper createModelMapper() {
        ModelMapper mapper = new ModelMapper();
        mapper.getConfiguration().setMatchingStrategy(MatchingStrategies.STRICT);
        return mapper;
    }
}
