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
@Table(name = "house_shared_amenities")
public class HouseSharedAmenity {
    @Id
    private String id;

    @Column(name = "house_id", nullable = false)
    private String houseId;

    @Column(name = "amenity_id", nullable = false)
    private String amenityId;

    @Column
    private Integer quantity;

    @Column(length = 255)
    private String notes;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // Transient for joined data
    @Transient
    private Amenity amenity;
}
