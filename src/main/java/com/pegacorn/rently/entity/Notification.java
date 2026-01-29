package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    private String id;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private NotificationType type;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String message;

    @Column(columnDefinition = "JSON")
    private String data;

    @Column(name = "is_read")
    private Boolean isRead;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        INVOICE_CREATED,
        PAYMENT_RECEIVED,
        PAYMENT_CONFIRMED,
        CONTRACT_EXPIRING,
        CONTRACT_EXPIRED,
        METER_READING_DUE,
        TENANT_ADDED,
        TENANT_LEFT,
        MAINTENANCE_REQUEST,
        SYSTEM_ANNOUNCEMENT
    }
}
