package com.pegacorn.rently.dto.push;

import java.util.Map;

public record PushNotificationRequest(
        String title,
        String body,
        String icon,
        String badge,
        String tag,
        Map<String, Object> data,
        boolean requireInteraction
) {
    public static PushNotificationRequest simple(String title, String body) {
        return new PushNotificationRequest(title, body, null, null, null, null, false);
    }

    public static PushNotificationRequest withUrl(String title, String body, String url) {
        return new PushNotificationRequest(title, body, null, null, null, Map.of("url", url), false);
    }
}
