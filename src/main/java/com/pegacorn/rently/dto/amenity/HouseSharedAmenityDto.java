package com.pegacorn.rently.dto.amenity;

import com.pegacorn.rently.entity.HouseSharedAmenity;

import java.time.LocalDateTime;

public record HouseSharedAmenityDto(
        String id,
        String houseId,
        String amenityId,
        String amenityName,
        String amenityIcon,
        String amenityCategory,
        LocalDateTime createdAt,
        Integer quantity,
        String notes) {
    public static HouseSharedAmenityDto fromEntity(HouseSharedAmenity hsa) {
        return new HouseSharedAmenityDto(
                hsa.getId(),
                hsa.getHouseId(),
                hsa.getAmenityId(),
                hsa.getAmenity() != null ? hsa.getAmenity().getName() : null,
                hsa.getAmenity() != null ? hsa.getAmenity().getIcon() : null,
                hsa.getAmenity() != null ? hsa.getAmenity().getCategory().name() : null,
                hsa.getCreatedAt(),
                hsa.getQuantity(),
                hsa.getNotes());
    }
}
