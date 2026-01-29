package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "rooms")
public class Room {
    @Id
    private String id;

    @Column(name = "house_id", nullable = false)
    private String houseId;

    @Column(nullable = false, length = 20)
    private String code;

    @Column(nullable = false)
    private int floor;

    @Column(name = "area_m2", nullable = false, precision = 10, scale = 2)
    private BigDecimal areaM2;

    @Column(name = "base_rent", nullable = false, precision = 15, scale = 2)
    private BigDecimal baseRent;

    @Column(name = "max_tenants")
    private Integer maxTenants;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RoomStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private List<RoomTenant> tenants;

    @Transient
    private Contract currentContract;

    @Transient
    private BigDecimal debt;

    public enum RoomStatus {
        EMPTY, RESERVED, RENTED, MAINTENANCE
    }
}
