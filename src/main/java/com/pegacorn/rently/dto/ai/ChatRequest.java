package com.pegacorn.rently.dto.ai;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record ChatRequest(
        @NotBlank(message = "Message is required")
        String message,
        List<ChatMessageDto> history
) {}
