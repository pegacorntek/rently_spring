package com.pegacorn.rently.dto.push;

import jakarta.validation.constraints.NotBlank;

public record PushSubscriptionRequest(
        @NotBlank(message = "Endpoint is required")
        String endpoint,

        @NotBlank(message = "Keys are required")
        Keys keys
) {
    public record Keys(
            @NotBlank(message = "p256dh key is required")
            String p256dh,

            @NotBlank(message = "auth key is required")
            String auth
    ) {}
}
