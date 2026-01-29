package com.pegacorn.rently.service;

import com.google.firebase.messaging.*;
import com.pegacorn.rently.dto.push.PushNotificationRequest;
import com.pegacorn.rently.dto.push.PushSubscriptionRequest;
import com.pegacorn.rently.entity.PushSubscription;
import com.pegacorn.rently.repository.PushSubscriptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushNotificationService {

    private final PushSubscriptionRepository subscriptionRepository;

    @Transactional
    public void registerToken(String userId, String fcmToken, String deviceName, String userAgent) {
        var existing = subscriptionRepository.findByUserIdAndFcmToken(userId, fcmToken);

        if (existing.isPresent()) {
            PushSubscription subscription = existing.get();
            subscription.setIsActive(true);
            subscription.setDeviceName(deviceName);
            subscription.setUserAgent(userAgent);
            subscription.setUpdatedAt(LocalDateTime.now());
            subscriptionRepository.save(subscription);
            log.info("Updated push subscription for user: {}", userId);
        } else {
            PushSubscription subscription = PushSubscription.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(userId)
                    .fcmToken(fcmToken)
                    .deviceName(deviceName)
                    .userAgent(userAgent)
                    .isActive(true)
                    .createdAt(LocalDateTime.now())
                    .updatedAt(LocalDateTime.now())
                    .build();
            subscriptionRepository.save(subscription);
            log.info("New push subscription saved for user: {}", userId);
        }
    }

    @Transactional
    public void unregisterToken(String fcmToken) {
        subscriptionRepository.deleteByFcmToken(fcmToken);
        log.info("Push subscription removed for token");
    }

    /**
     * Legacy method for VAPID-style subscription (stores endpoint as fcmToken for compatibility)
     */
    @Transactional
    public void subscribe(String userId, PushSubscriptionRequest request) {
        registerToken(userId, request.endpoint(), "Web Browser", null);
    }

    /**
     * Legacy method for VAPID-style unsubscription
     */
    @Transactional
    public void unsubscribe(String endpoint) {
        unregisterToken(endpoint);
    }

    @Async
    public void sendToUser(String userId, String title, String body, Map<String, Object> data) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserIdAndIsActiveTrue(userId);

        for (PushSubscription subscription : subscriptions) {
            sendPushNotification(subscription, title, body, data);
        }
    }

    @Async
    public void sendToUser(String userId, PushNotificationRequest request) {
        sendToUser(userId, request.title(), request.body(), request.data());
    }

    @Async
    public void sendToAllUsers(String title, String body, Map<String, Object> data) {
        List<PushSubscription> subscriptions = subscriptionRepository.findAllActive();

        for (PushSubscription subscription : subscriptions) {
            sendPushNotification(subscription, title, body, data);
        }
    }

    @Async
    public void sendToAll(PushNotificationRequest request) {
        sendToAllUsers(request.title(), request.body(), request.data());
    }

    private void sendPushNotification(PushSubscription subscription, String title, String body, Map<String, Object> data) {
        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(subscription.getFcmToken())
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .setWebpushConfig(WebpushConfig.builder()
                            .setNotification(WebpushNotification.builder()
                                    .setTitle(title)
                                    .setBody(body)
                                    .setIcon("/logo-192.png")
                                    .setBadge("/badge.png")
                                    .build())
                            .build());

            if (data != null) {
                data.forEach((key, value) -> messageBuilder.putData(key, String.valueOf(value)));
            }

            String response = FirebaseMessaging.getInstance().send(messageBuilder.build());
            log.debug("Push notification sent: {}", response);
        } catch (FirebaseMessagingException e) {
            handleFirebaseException(subscription, e);
        }
    }

    private void handleFirebaseException(PushSubscription subscription, FirebaseMessagingException e) {
        log.error("Failed to send push notification: {}", e.getMessage());

        if (e.getMessagingErrorCode() == MessagingErrorCode.UNREGISTERED ||
            e.getMessagingErrorCode() == MessagingErrorCode.INVALID_ARGUMENT) {
            subscription.setIsActive(false);
            subscriptionRepository.save(subscription);
            log.info("Deactivated invalid FCM token for user {}", subscription.getUserId());
        }
    }

    public boolean hasSubscription(String userId) {
        return !subscriptionRepository.findByUserIdAndIsActiveTrue(userId).isEmpty();
    }
}
