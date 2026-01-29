package com.pegacorn.rently.dto.setting;

import com.pegacorn.rently.entity.SystemSetting;

import java.time.LocalDateTime;

public record SystemSettingDto(
    String id,
    String key,
    String value,
    String description,
    String valueType,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static SystemSettingDto fromEntity(SystemSetting entity) {
        return new SystemSettingDto(
            entity.getId(),
            entity.getKey(),
            entity.getValue(),
            entity.getDescription(),
            entity.getValueType(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
