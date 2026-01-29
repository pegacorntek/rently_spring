package com.pegacorn.rently.dto.ai;

public record ChatResponseDto(
        String message,       // AI response text
        String actionType,    // null | "CREATE_INVOICE" | "SHOW_EXPENSE" | "SHOW_INCOME"
        Object actionData     // Prepared data for confirmation (e.g., invoice preview)
) {}
