package com.pegacorn.rently.controller;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.announcement.AnnouncementDto;
import com.pegacorn.rently.dto.auth.ChangePasswordRequest;
import com.pegacorn.rently.dto.auth.UpdateBankSettingsRequest;
import com.pegacorn.rently.dto.auth.UpdateProfileRequest;
import com.pegacorn.rently.dto.auth.UserDto;
import com.pegacorn.rently.dto.common.ApiResponse;
import com.pegacorn.rently.dto.invoice.InvoiceDto;
import com.pegacorn.rently.dto.payment.VietQRResponse;
import com.pegacorn.rently.dto.push.PushNotificationRequest;
import com.pegacorn.rently.dto.push.PushSubscriptionRequest;
import com.pegacorn.rently.security.UserPrincipal;
import com.pegacorn.rently.service.AdminService;
import com.pegacorn.rently.service.AuthService;
import com.pegacorn.rently.service.InvoiceService;
import com.pegacorn.rently.service.PushNotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class CommonController {

    private final AuthService authService;
    private final InvoiceService invoiceService;
    private final PushNotificationService pushNotificationService;
    private final AdminService adminService;

    // ==================== ME (Current User Profile) ====================

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> getMe(@AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.getMe(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/me")
    public ResponseEntity<ApiResponse<UserDto>> updateProfile(
            @Valid @RequestBody UpdateProfileRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.updateProfile(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(user, MessageConstant.PROFILE_UPDATED_SUCCESS));
    }

    @PutMapping("/me/bank-settings")
    public ResponseEntity<ApiResponse<UserDto>> updateBankSettings(
            @Valid @RequestBody UpdateBankSettingsRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        UserDto user = authService.updateBankSettings(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(user, MessageConstant.BANK_SETTINGS_UPDATED_SUCCESS));
    }

    // ==================== PUBLIC INVOICES ====================

    @GetMapping("/public/invoices/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getPublicInvoice(@PathVariable String id) {
        InvoiceDto invoice = invoiceService.getPublicInvoice(id);
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping("/public/invoices/{id}/vietqr")
    public ResponseEntity<ApiResponse<VietQRResponse>> getPublicInvoiceVietQR(@PathVariable String id) {
        VietQRResponse qrData = invoiceService.generateVietQR(id);
        return ResponseEntity.ok(ApiResponse.success(qrData));
    }

    // ==================== PUSH NOTIFICATIONS ====================

    @PostMapping("/push/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribePush(
            @Valid @RequestBody PushSubscriptionRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        pushNotificationService.subscribe(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.PUSH_SUBSCRIBED_SUCCESS));
    }

    @PostMapping("/push/unsubscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribePush(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String endpoint = request.get("endpoint");
        if (endpoint != null) {
            pushNotificationService.unsubscribe(endpoint);
        }
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.PUSH_UNSUBSCRIBED_SUCCESS));
    }

    @GetMapping("/push/status")
    public ResponseEntity<ApiResponse<Map<String, Boolean>>> getPushStatus(
            @AuthenticationPrincipal UserPrincipal principal) {
        boolean hasSubscription = pushNotificationService.hasSubscription(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("subscribed", hasSubscription)));
    }

    @PostMapping("/push/send")
    public ResponseEntity<ApiResponse<Void>> sendPushNotification(
            @RequestBody PushNotificationRequest request,
            @RequestParam(required = false) String userId) {
        if (userId != null) {
            pushNotificationService.sendToUser(userId, request);
        } else {
            pushNotificationService.sendToAll(request);
        }
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.PUSH_SENT_SUCCESS));
    }

    @PostMapping("/push/test")
    public ResponseEntity<ApiResponse<Void>> testPushNotification(
            @AuthenticationPrincipal UserPrincipal principal) {
        PushNotificationRequest notification = PushNotificationRequest.withUrl(
                "Thông báo thử nghiệm",
                "Đây là thông báo thử nghiệm từ Rently",
                "/landlord");
        pushNotificationService.sendToUser(principal.getId(), notification);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.PUSH_TEST_SENT_SUCCESS));
    }

    // ==================== ANNOUNCEMENTS (Public) ====================

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<List<AnnouncementDto>>> getActiveAnnouncements(
            @RequestParam(defaultValue = "ALL") String audience) {
        List<AnnouncementDto> announcements = adminService.getActiveAnnouncements(audience);
        return ResponseEntity.ok(ApiResponse.success(announcements));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        authService.changePassword(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.CHANGE_PASSWORD_SUCCESS));
    }
}
