package com.pegacorn.rently.dto.invoice;

import com.pegacorn.rently.entity.UtilityShortfall;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UtilityShortfallDto(
        String id,
        String houseId,
        String periodMonth,
        BigDecimal electricityShortfall,
        BigDecimal waterShortfall,
        BigDecimal totalShortfall,
        BigDecimal perRoomAmount,
        int activeRoomCount,
        String status,
        LocalDateTime createdAt,
        LocalDateTime appliedAt
) {
    public static UtilityShortfallDto fromEntity(UtilityShortfall entity) {
        return new UtilityShortfallDto(
                entity.getId(),
                entity.getHouseId(),
                entity.getPeriodMonth(),
                entity.getElectricityShortfall(),
                entity.getWaterShortfall(),
                entity.getTotalShortfall(),
                entity.getPerRoomAmount(),
                entity.getActiveRoomCount(),
                entity.getStatus().name(),
                entity.getCreatedAt(),
                entity.getAppliedAt()
        );
    }
}
