package com.pegacorn.rently.controller;

import com.pegacorn.rently.dto.common.ApiResponse;
import com.pegacorn.rently.dto.notification.NotificationDto;
import com.pegacorn.rently.dto.notification.RegisterPushTokenRequest;
import com.pegacorn.rently.security.UserPrincipal;
import com.pegacorn.rently.service.NotificationService;
import com.pegacorn.rently.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;
    private final PushNotificationService pushNotificationService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<NotificationDto>>> getNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Page<NotificationDto> notifications = notificationService.getNotifications(
                userPrincipal.getId(), page, size
        );
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread")
    public ResponseEntity<ApiResponse<List<NotificationDto>>> getUnreadNotifications(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        List<NotificationDto> notifications = notificationService.getUnreadNotifications(
                userPrincipal.getId()
        );
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        long count = notificationService.getUnreadCount(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NotificationDto>> getNotification(
            @PathVariable String id
    ) {
        return notificationService.getNotification(id)
                .map(n -> ResponseEntity.ok(ApiResponse.success(n)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable String id) {
        notificationService.markAsRead(id);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PatchMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        notificationService.markAllAsRead(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/push/register")
    public ResponseEntity<ApiResponse<Void>> registerPushToken(
            @AuthenticationPrincipal UserPrincipal userPrincipal,
            @Valid @RequestBody RegisterPushTokenRequest request,
            @RequestHeader(value = "User-Agent", required = false) String userAgent
    ) {
        pushNotificationService.registerToken(
                userPrincipal.getId(),
                request.fcmToken(),
                request.deviceName(),
                userAgent
        );
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @DeleteMapping("/push/unregister")
    public ResponseEntity<ApiResponse<Void>> unregisterPushToken(
            @RequestParam String fcmToken
    ) {
        pushNotificationService.unregisterToken(fcmToken);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @GetMapping("/push/status")
    public ResponseEntity<ApiResponse<Boolean>> getPushStatus(
            @AuthenticationPrincipal UserPrincipal userPrincipal
    ) {
        boolean hasSubscription = pushNotificationService.hasSubscription(userPrincipal.getId());
        return ResponseEntity.ok(ApiResponse.success(hasSubscription));
    }
}
