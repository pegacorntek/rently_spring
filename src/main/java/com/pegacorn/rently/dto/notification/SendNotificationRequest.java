package com.pegacorn.rently.dto.notification;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

public record SendNotificationRequest(
        @NotBlank(message = "Tiêu đề không được để trống")
        String title,

        @NotBlank(message = "Nội dung không được để trống")
        String message,

        // Target: "ALL", "LANDLORDS", "TENANTS", or list of specific user IDs
        String targetAudience,

        // Optional: specific user IDs (if targetAudience is null/empty)
        List<String> userIds
) {}
