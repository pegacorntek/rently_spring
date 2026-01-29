package com.pegacorn.rently.dto.announcement;

import com.pegacorn.rently.entity.Announcement;

import java.time.LocalDateTime;

public record AnnouncementDto(
    String id,
    String title,
    String content,
    String type,
    String status,
    String targetAudience,
    LocalDateTime publishAt,
    LocalDateTime expireAt,
    String createdBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static AnnouncementDto fromEntity(Announcement entity) {
        return new AnnouncementDto(
            entity.getId(),
            entity.getTitle(),
            entity.getContent(),
            entity.getType().name(),
            entity.getStatus().name(),
            entity.getTargetAudience().name(),
            entity.getPublishAt(),
            entity.getExpireAt(),
            entity.getCreatedBy(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }
}
