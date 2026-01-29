package com.pegacorn.rently.dto.ai;

public record ChatMessageDto(
        String role,    // "user" or "assistant"
        String content
) {}
