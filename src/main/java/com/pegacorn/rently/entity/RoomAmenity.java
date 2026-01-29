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
@Table(name = "room_amenities")
public class RoomAmenity {
    @Id
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "amenity_id", nullable = false)
    private String amenityId;

    @Column
    private Integer quantity;

    @Column(length = 255)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(name = "`condition`", length = 20)
    private AmenityCondition condition;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "amenity_id", insertable = false, updatable = false)
    private Amenity amenity;
}
