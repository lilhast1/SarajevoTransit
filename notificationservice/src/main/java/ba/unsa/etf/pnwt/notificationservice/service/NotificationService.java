package ba.unsa.etf.pnwt.notificationservice.service;

import ba.unsa.etf.pnwt.notificationservice.dto.CreateNotificationRequest;
import ba.unsa.etf.pnwt.notificationservice.dto.NotificationResponse;
import ba.unsa.etf.pnwt.notificationservice.model.Notification;
import ba.unsa.etf.pnwt.notificationservice.repository.NotificationRepository;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;

import ba.unsa.etf.pnwt.notificationservice.exception.NotFoundException;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final ModelMapper modelMapper;

    public NotificationService(NotificationRepository notificationRepository, ModelMapper modelMapper) {
        this.notificationRepository = notificationRepository;
        this.modelMapper = modelMapper;
    }

    public List<NotificationResponse> getAll() {
        return notificationRepository.findAll().stream()
                .map(n -> modelMapper.map(n, NotificationResponse.class))
                .toList();
    }

    public NotificationResponse getById(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        return modelMapper.map(notification, NotificationResponse.class);
    }

    public List<NotificationResponse> getByUserId(Long userId) {
        return notificationRepository.findByUserId(userId).stream()
                .map(n -> modelMapper.map(n, NotificationResponse.class))
                .toList();
    }

    public List<NotificationResponse> getUnreadByUserId(Long userId) {
        return notificationRepository.findByUserIdAndIsRead(userId, false).stream()
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

    public NotificationResponse markAsRead(Long id) {
        Notification notification = notificationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Notification not found: " + id));
        notification.setIsRead(true);
        return modelMapper.map(notificationRepository.save(notification), NotificationResponse.class);
    }

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
