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
@Table(name = "service_fees")
public class ServiceFee {
    @Id
    private String id;

    @Column(name = "house_id", nullable = false)
    private String houseId;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false)
    private FeeType feeType;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(length = 50)
    private String unit;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum FeeType {
        FIXED,           // Fixed amount per room per month
        PER_PERSON,      // Amount multiplied by number of tenants in room
        SPLIT_EQUAL,     // Total bill divided equally among all rooms
        SPLIT_BY_TENANT  // Total bill divided by total tenants, room pays based on their tenant count
    }
}
