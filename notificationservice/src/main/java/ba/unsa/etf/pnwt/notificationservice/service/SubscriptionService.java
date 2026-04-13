package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.SubscriptionResponse;
import ba.unsa.etf.pnwt.notificationservice.dto.UpdateSubscriptionRequest;
import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
import ba.unsa.etf.pnwt.notificationservice.repository.SubscriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;

import java.util.List;
import java.util.UUID;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final ModelMapper modelMapper;

    public SubscriptionService(SubscriptionRepository subscriptionRepository, ModelMapper modelMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.modelMapper = modelMapper;
    }

    public List<SubscriptionResponse> getAll() {
        return subscriptionRepository.findAll().stream()
                .map(s -> modelMapper.map(s, SubscriptionResponse.class))
                .toList();
    }

    public SubscriptionResponse getById(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription not found: " + id));
        return modelMapper.map(subscription, SubscriptionResponse.class);
    }

    public List<SubscriptionResponse> getByUserId(UUID userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(s -> modelMapper.map(s, SubscriptionResponse.class))
                .toList();
    }

    public List<SubscriptionResponse> getByLineId(UUID lineId) {
        return subscriptionRepository.findByLineId(lineId).stream()
                .map(s -> modelMapper.map(s, SubscriptionResponse.class))
                .toList();
    }

    public List<SubscriptionResponse> getActiveByUserId(UUID userId) {
        return subscriptionRepository.findByUserIdAndIsActive(userId, true).stream()
                .map(s -> modelMapper.map(s, SubscriptionResponse.class))
                .toList();
    }

    public List<SubscriptionResponse> searchByName(String name) {
        return subscriptionRepository.findByUserFullNameContainingIgnoreCase(name).stream()
                .map(s -> modelMapper.map(s, SubscriptionResponse.class))
                .toList();
    }

    public List<SubscriptionResponse> searchByEmail(String email) {
        return subscriptionRepository.findByUserEmailIgnoreCase(email).stream()
                .map(s -> modelMapper.map(s, SubscriptionResponse.class))
                .toList();
    }

    public SubscriptionResponse create(CreateSubscriptionRequest request) {
        Subscription subscription = modelMapper.map(request, Subscription.class);
        subscription.setIsActive(true);
        Subscription saved = subscriptionRepository.save(subscription);
        return modelMapper.map(saved, SubscriptionResponse.class);
    }

    public SubscriptionResponse deactivate(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription not found: " + id));
        subscription.setIsActive(false);
        return modelMapper.map(subscriptionRepository.save(subscription), SubscriptionResponse.class);
    }

    public SubscriptionResponse activate(UUID id) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription not found: " + id));
        subscription.setIsActive(true);
        return modelMapper.map(subscriptionRepository.save(subscription), SubscriptionResponse.class);
    }

    public SubscriptionResponse update(UUID id, UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Subscription not found: " + id));
        if (request.getLineId() != null) subscription.setLineId(request.getLineId());
        if (request.getLineCode() != null) subscription.setLineCode(request.getLineCode());
        if (request.getLineName() != null) subscription.setLineName(request.getLineName());
        if (request.getStartInterval() != null) subscription.setStartInterval(request.getStartInterval());
        if (request.getEndInterval() != null) subscription.setEndInterval(request.getEndInterval());
        if (request.getDaysOfWeek() != null) subscription.setDaysOfWeek(request.getDaysOfWeek());
        return modelMapper.map(subscriptionRepository.save(subscription), SubscriptionResponse.class);
    }

    public void delete(UUID id) {
        if (!subscriptionRepository.existsById(id)) {
            throw new NotFoundException("Subscription not found: " + id);
        }
        subscriptionRepository.deleteById(id);
    }
}
