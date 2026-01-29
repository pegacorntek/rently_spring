package com.pegacorn.rently.dto.task;

import com.pegacorn.rently.entity.Task;

import java.time.LocalDateTime;

public record TaskDto(
        String id,
        String title,
        boolean isDone,
        boolean isPinned,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static TaskDto fromEntity(Task task) {
        return new TaskDto(
                task.getId(),
                task.getTitle(),
                task.isDone(),
                task.isPinned(),
                task.getCreatedAt(),
                task.getUpdatedAt()
        );
    }
}
