package com.pegacorn.rently.dto.house;

import com.pegacorn.rently.entity.House;

import java.time.LocalDateTime;

public record HouseDto(
        String id,
        String ownerId,
        String name,
        String address,
        String description,
        String status,
        int roomCount,
        int tenantCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static HouseDto fromEntity(House house) {
        return new HouseDto(
                house.getId(),
                house.getOwnerId(),
                house.getName(),
                house.getAddress(),
                house.getDescription(),
                house.getStatus().name(),
                house.getRoomCount(),
                house.getTenantCount(),
                house.getCreatedAt(),
                house.getUpdatedAt()
        );
    }
}
