package com.pegacorn.rently.controller;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.common.ApiResponse;
import com.pegacorn.rently.dto.payment.SepayWebhookRequest;
import com.pegacorn.rently.service.SepayService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * SePay webhook endpoint for receiving bank transfer notifications.
 */
@Slf4j
@RestController
@RequestMapping("/public/sepay")
@RequiredArgsConstructor
public class SepayWebhookController {

    private final SepayService sepayService;

    /**
     * SePay webhook endpoint.
     * Called by SePay when a bank transfer is received.
     */
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<Void>> handleWebhook(
            @RequestBody SepayWebhookRequest request,
            @RequestHeader(value = "Authorization", required = false) String authorization,
            @RequestHeader(value = "X-API-Key", required = false) String apiKey
    ) {
        log.info("SePay webhook: id={}, amount={}, code={}",
                request.id(), request.transferAmount(), request.code());

        String key = apiKey;
        if (key == null && authorization != null && authorization.startsWith("Bearer ")) {
            key = authorization.substring(7);
        }

        try {
            sepayService.processWebhook(request, key);
            return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.SEPAY_WEBHOOK_OK));
        } catch (Exception e) {
            log.error("Webhook error: {}", e.getMessage());
            return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.SEPAY_WEBHOOK_RECEIVED));
        }
    }
}
