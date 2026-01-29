package com.pegacorn.rently.dto.amenity;

import com.pegacorn.rently.entity.AmenityCondition;
import com.pegacorn.rently.entity.RoomAmenity;

import java.time.LocalDate;

public record RoomAmenityDto(
        String id,
        String roomId,
        String amenityId,
        AmenityDto amenity,
        Integer quantity,
        String notes,
        AmenityCondition condition) {
    public static RoomAmenityDto from(RoomAmenity ra) {
        return new RoomAmenityDto(
                ra.getId(),
                ra.getRoomId(),
                ra.getAmenityId(),
                ra.getAmenity() != null ? AmenityDto.from(ra.getAmenity()) : null,
                ra.getQuantity(),
                ra.getNotes(),
                ra.getCondition());
    }
}
