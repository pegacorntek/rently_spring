package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "utility_shortfalls")
public class UtilityShortfall {

    public enum Status {
        PENDING,
        APPLIED
    }

    @Id
    private String id;

    @Column(name = "house_id", nullable = false)
    private String houseId;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth; // The month that had the shortfall (e.g., "2026-01")

    @Column(name = "electricity_shortfall", nullable = false, precision = 15, scale = 2)
    private BigDecimal electricityShortfall;

    @Column(name = "water_shortfall", nullable = false, precision = 15, scale = 2)
    private BigDecimal waterShortfall;

    @Column(name = "total_shortfall", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalShortfall;

    @Column(name = "per_room_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal perRoomAmount;

    @Column(name = "active_room_count", nullable = false)
    private Integer activeRoomCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "applied_at")
    private LocalDateTime appliedAt;
}
