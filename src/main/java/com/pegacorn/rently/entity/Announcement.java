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
@Table(name = "announcements")
public class Announcement {
    @Id
    private String id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnnouncementStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TargetAudience targetAudience;

    @Column(name = "publish_at")
    private LocalDateTime publishAt;

    @Column(name = "expire_at")
    private LocalDateTime expireAt;

    @Column(name = "created_by", nullable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum AnnouncementType {
        INFO,
        WARNING,
        URGENT,
        MAINTENANCE
    }

    public enum AnnouncementStatus {
        DRAFT,
        PUBLISHED,
        ARCHIVED
    }

    public enum TargetAudience {
        ALL,
        LANDLORDS,
        TENANTS
    }
}
