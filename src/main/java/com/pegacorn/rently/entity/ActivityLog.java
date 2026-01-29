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
@Table(name = "activity_logs")
public class ActivityLog {
    @Id
    private String id;

    @Column(name = "landlord_id", nullable = false)
    private String landlordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ActivityType type;

    @Column(name = "entity_id")
    private String entityId;

    @Column(name = "entity_type")
    private String entityType;

    @Column(nullable = false)
    private String description;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum ActivityType {
        INVOICE_CREATED,
        INVOICE_SENT,
        INVOICE_PAID,
        INVOICE_CANCELLED,
        CONTRACT_CREATED,
        CONTRACT_SIGNED,
        CONTRACT_ENDED,
        DEPOSIT_CONFIRMED,
        ROOM_CREATED,
        ROOM_STATUS_CHANGED,
        TENANT_ADDED,
        HOUSE_CREATED,
        METER_READING_SAVED,
        PAYMENT_RECORDED,
        USER_LOGIN,
        USER_LOGOUT
    }
}
