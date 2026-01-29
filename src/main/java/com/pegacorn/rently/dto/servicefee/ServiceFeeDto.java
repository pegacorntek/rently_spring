package com.pegacorn.rently.dto.servicefee;

import com.pegacorn.rently.entity.ServiceFee;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ServiceFeeDto(
        String id,
        String houseId,
        String name,
        String feeType,
        BigDecimal amount,
        String unit,
        boolean isActive,
        Integer displayOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ServiceFeeDto fromEntity(ServiceFee fee) {
        return new ServiceFeeDto(
                fee.getId(),
                fee.getHouseId(),
                fee.getName(),
                fee.getFeeType().name(),
                fee.getAmount(),
                fee.getUnit(),
                fee.isActive(),
                fee.getDisplayOrder(),
                fee.getCreatedAt(),
                fee.getUpdatedAt()
        );
    }
}
