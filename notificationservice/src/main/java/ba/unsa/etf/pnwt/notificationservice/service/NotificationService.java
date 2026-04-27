package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.*;
import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;
import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import ba.unsa.etf.pnwt.notificationservice.model.Subscription;
import ba.unsa.etf.pnwt.notificationservice.repository.NotificationRepository;
import ba.unsa.etf.pnwt.notificationservice.repository.SubscriptionRepository;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ModelMapper modelMapper;

    public NotificationService(NotificationRepository notificationRepository,
                                SubscriptionRepository subscriptionRepository,
                                ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.modelMapper = modelMapper;
    }

    public PagedResponse<NotificationResponse> getAll(Pageable pageable) {
        return PagedResponse.of(
                notificationRepository.findAll(pageable)
                        .map(n -> modelMapper.map(n, NotificationResponse.class))
        );
    }

    public NotificationResponse getById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        return modelMapper.map(notification, NotificationResponse.class);
    }

    public PagedResponse<NotificationResponse> getByUserId(Long userId, Pageable pageable) {
        return PagedResponse.of(
                notificationRepository.findByUserId(userId, pageable)
                        .map(n -> modelMapper.map(n, NotificationResponse.class))
        );
    }

    public List<NotificationResponse> getUnreadByUserId(Long userId) {
        return notificationRepository.findByUserIdAndIsRead(userId, false).stream()
                .map(n -> modelMapper.map(n, NotificationResponse.class))
                .toList();
    }

    public long countUnreadByUserId(Long userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public List<NotificationResponse> getByUserIdAndDateRange(Long userId, LocalDateTime from, LocalDateTime to) {
        return notificationRepository.findByUserIdAndSentAtBetween(userId, from, to).stream()
                .map(n -> modelMapper.map(n, NotificationResponse.class))
                .toList();
    }

    public NotificationResponse create(CreateNotificationRequest request) {
        Notification notification = modelMapper.map(request, Notification.class);
        notification.setSentAt(LocalDateTime.now());
        notification.setIsRead(false);
        Notification saved = notificationRepository.save(notification);
        return modelMapper.map(saved, NotificationResponse.class);
    }

    @Transactional
    public List<NotificationResponse> createBatch(BatchCreateNotificationRequest batchRequest) {
        LocalDateTime now = LocalDateTime.now();
        List<Notification> entities = batchRequest.getNotifications().stream()
                .map(req -> {
                    Notification n = modelMapper.map(req, Notification.class);
                    n.setSentAt(now);
                    n.setIsRead(false);
                    return n;
                })
                .toList();
        return notificationRepository.saveAll(entities).stream()
                .map(n -> modelMapper.map(n, NotificationResponse.class))
                .toList();
    }

    @Transactional
    public BroadcastNotificationResponse broadcast(BroadcastNotificationRequest request) {
        List<Subscription> subscribers = subscriptionRepository.findByLineIdAndIsActive(request.getLineId(), true);
        if (subscribers.isEmpty()) {
            return new BroadcastNotificationResponse(0, request.getLineId(), null, null);
        }
        LocalDateTime now = LocalDateTime.now();
        List<Notification> notifications = subscribers.stream().map(sub -> {
            Notification n = new Notification();
            n.setUserId(sub.getUserId());
            n.setUserFullName(sub.getUserFullName());
            n.setUserEmail(sub.getUserEmail());
            n.setLineId(sub.getLineId());
            n.setLineCode(sub.getLineCode());
            n.setLineName(sub.getLineName());
            n.setType(request.getType());
            n.setTitle(request.getTitle());
            n.setContent(request.getContent());
            n.setIsRead(false);
            n.setSentAt(now);
            return n;
        }).toList();
        List<Notification> saved = notificationRepository.saveAll(notifications);
        Subscription first = subscribers.get(0);
        return new BroadcastNotificationResponse(saved.size(), first.getLineId(), first.getLineCode(), first.getLineName());
    }

    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        notification.setIsRead(true);
        return modelMapper.map(notificationRepository.save(notification), NotificationResponse.class);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository.findByUserIdAndIsRead(userId, false);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    public void delete(Long id) {
        if (!notificationRepository.existsById(id)) {
            throw new NotFoundException("Notification not found: " + id);
        }
        notificationRepository.deleteById(id);
    }
}
