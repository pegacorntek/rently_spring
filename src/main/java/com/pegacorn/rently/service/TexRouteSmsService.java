package com.pegacorn.rently.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TexRouteSmsService implements SmsService {

    private static final String TEXROUTE_API_URL = "https://api.texroute.net/api/v1/sms/send";

    @Value("${texroute.api-key:}")
    private String apiKey;

    @Value("${texroute.device-id:}")
    private String deviceId;

    @Value("${texroute.enabled:false}")
    private boolean enabled;

    private final RestTemplate restTemplate;

    public TexRouteSmsService() {
        this.restTemplate = new RestTemplate();
    }

    @Override
    public boolean sendSms(String phone, String message) {
        if (!enabled) {
            log.info("[SMS-DEV] Would send to {}: {}", phone, message);
            return true;
        }

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("TexRoute API key not configured");
            return false;
        }

        if (deviceId == null || deviceId.isBlank()) {
            log.warn("TexRoute device ID not configured");
            return false;
        }

        try {
            String formattedPhone = formatPhoneNumber(phone);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-API-Key", apiKey);

            Map<String, String> body = new HashMap<>();
            body.put("phone", formattedPhone);
            body.put("message", message);
            body.put("deviceId", deviceId);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    TEXROUTE_API_URL,
                    HttpMethod.POST,
                    request,
                    Map.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully to {}", formattedPhone);
                return true;
            } else {
                log.error("Failed to send SMS. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                return false;
            }
        } catch (Exception e) {
            log.error("Error sending SMS to {}: {}", phone, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Format Vietnamese phone number to international format
     * 0901234567 -> +84901234567
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null) return null;

        String cleaned = phone.replaceAll("[^0-9+]", "");

        if (cleaned.startsWith("+")) {
            return cleaned;
        }

        if (cleaned.startsWith("0")) {
            return "+84" + cleaned.substring(1);
        }

        if (cleaned.startsWith("84")) {
            return "+" + cleaned;
        }

        return "+84" + cleaned;
    }
}
