package com.pegacorn.rently.dto.ai;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.Map;

public record ExecuteActionRequest(
    @NotBlank(message = "Action type is required")
    String actionType,

    @NotNull(message = "Action data is required")
    Map<String, Object> actionData
) {}
