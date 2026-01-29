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
@Table(name = "amenities")
public class Amenity {
    @Id
    private String id;

    @Column(name = "house_id")
    private String houseId; // null = predefined/global amenity

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AmenityCategory category;

    @Column(length = 50)
    private String icon;

    @Column(name = "is_custom", nullable = false)
    private boolean isCustom;

    @Column(precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum AmenityCategory {
        FURNITURE, APPLIANCE, UTILITY, FACILITY, OTHER
    }
}
