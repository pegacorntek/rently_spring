package com.pegacorn.rently.service;

import com.pegacorn.rently.dto.notification.NotificationDto;
import com.pegacorn.rently.entity.Notification;
import com.pegacorn.rently.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final PushNotificationService pushNotificationService;

    public Page<NotificationDto> getNotifications(String userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(NotificationDto::from);
    }

    public List<NotificationDto> getUnreadNotifications(String userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(NotificationDto::from)
                .toList();
    }

    public long getUnreadCount(String userId) {
        return notificationRepository.countUnreadByUserId(userId);
    }

    public Optional<NotificationDto> getNotification(String id) {
        return notificationRepository.findById(id).map(NotificationDto::from);
    }

    @Transactional
    public void markAsRead(String id) {
        notificationRepository.markAsRead(id);
    }

    @Transactional
    public void markAllAsRead(String userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional
    public NotificationDto createNotification(
            String userId,
            Notification.NotificationType type,
            String title,
            String message,
            Map<String, Object> data
    ) {
        Notification notification = Notification.builder()
                .id(UUID.randomUUID().toString())
                .userId(userId)
                .type(type)
                .title(title)
                .message(message)
                .data(data != null ? convertMapToJson(data) : null)
                .isRead(false)
                .createdAt(LocalDateTime.now())
                .build();

        notificationRepository.save(notification);

        // Send push notification asynchronously
        pushNotificationService.sendToUser(userId, title, message, data);

        return NotificationDto.from(notification);
    }

    private String convertMapToJson(Map<String, Object> data) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(data);
        } catch (Exception e) {
            return null;
        }
    }
}
