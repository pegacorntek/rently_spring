package com.pegacorn.rently.dto.ai;

public record ExecuteActionResponse(
    boolean success,
    String message,
    Object data
) {
    public static ExecuteActionResponse success(String message, Object data) {
        return new ExecuteActionResponse(true, message, data);
    }

    public static ExecuteActionResponse success(String message) {
        return new ExecuteActionResponse(true, message, null);
    }

    public static ExecuteActionResponse error(String message) {
        return new ExecuteActionResponse(false, message, null);
    }
}
