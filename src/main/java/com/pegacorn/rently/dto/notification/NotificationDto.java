package com.pegacorn.rently.dto.notification;

import com.pegacorn.rently.entity.Notification;

import java.time.LocalDateTime;

public record NotificationDto(
        String id,
        String type,
        String title,
        String message,
        String data,
        Boolean isRead,
        LocalDateTime readAt,
        LocalDateTime createdAt
) {
    public static NotificationDto from(Notification notification) {
        return new NotificationDto(
                notification.getId(),
                notification.getType().name(),
                notification.getTitle(),
                notification.getMessage(),
                notification.getData(),
                notification.getIsRead(),
                notification.getReadAt(),
                notification.getCreatedAt()
        );
    }
}
