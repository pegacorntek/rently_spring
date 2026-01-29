package com.pegacorn.rently.dto.notification;

import jakarta.validation.constraints.NotBlank;

public record RegisterPushTokenRequest(
        @NotBlank(message = "FCM token is required")
        String fcmToken,
        String deviceName
) {}
