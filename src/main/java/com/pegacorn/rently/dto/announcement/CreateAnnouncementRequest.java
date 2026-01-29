package com.pegacorn.rently.dto.announcement;

import java.time.LocalDateTime;

public record CreateAnnouncementRequest(
    String title,
    String content,
    String type,
    String targetAudience,
    LocalDateTime publishAt,
    LocalDateTime expireAt
) {}
