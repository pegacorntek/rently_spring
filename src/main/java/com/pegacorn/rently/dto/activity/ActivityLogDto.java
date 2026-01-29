package com.pegacorn.rently.dto.activity;

import com.pegacorn.rently.entity.ActivityLog;

import java.time.LocalDateTime;

public record ActivityLogDto(
        String id,
        String type,
        String entityId,
        String entityType,
        String description,
        String metadata,
        LocalDateTime createdAt
) {
    public static ActivityLogDto fromEntity(ActivityLog activity) {
        return new ActivityLogDto(
                activity.getId(),
                activity.getType().name(),
                activity.getEntityId(),
                activity.getEntityType(),
                activity.getDescription(),
                activity.getMetadata(),
                activity.getCreatedAt()
        );
    }
}
