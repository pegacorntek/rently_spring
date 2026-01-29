package com.pegacorn.rently.dto.amenity;

import com.pegacorn.rently.entity.Amenity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AmenityDto(
        String id,
        String houseId,
        String name,
        Amenity.AmenityCategory category,
        String icon,
        boolean isCustom,
        BigDecimal price,
        LocalDateTime createdAt
) {
    public static AmenityDto from(Amenity amenity) {
        return new AmenityDto(
                amenity.getId(),
                amenity.getHouseId(),
                amenity.getName(),
                amenity.getCategory(),
                amenity.getIcon(),
                amenity.isCustom(),
                amenity.getPrice(),
                amenity.getCreatedAt()
        );
    }
}
