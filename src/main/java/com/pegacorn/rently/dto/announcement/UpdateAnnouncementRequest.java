package com.pegacorn.rently.dto.announcement;

import java.time.LocalDateTime;

public record UpdateAnnouncementRequest(
    String title,
    String content,
    String type,
    String status,
    String targetAudience,
    LocalDateTime publishAt,
    LocalDateTime expireAt
) {}
