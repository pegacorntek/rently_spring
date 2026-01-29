package com.pegacorn.rently.controller;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.announcement.AnnouncementDto;
import com.pegacorn.rently.dto.announcement.CreateAnnouncementRequest;
import com.pegacorn.rently.dto.announcement.UpdateAnnouncementRequest;
import com.pegacorn.rently.dto.auth.UserDto;
import com.pegacorn.rently.dto.common.ApiResponse;
import com.pegacorn.rently.dto.notification.SendNotificationRequest;
import com.pegacorn.rently.dto.setting.CreateSettingRequest;
import com.pegacorn.rently.dto.setting.SystemSettingDto;
import com.pegacorn.rently.dto.setting.UpdateSettingRequest;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.security.UserPrincipal;
import com.pegacorn.rently.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ==================== STATS ====================

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSystemStats() {
        Map<String, Object> stats = adminService.getSystemStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ==================== USERS ====================

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "false") boolean includeDeleted) {
        Map<String, Object> users = adminService.getAllUsersPaginated(page, size, status, role, search, includeDeleted);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable String id) {
        UserDto user = adminService.getUserById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PutMapping("/users/{id}/lock")
    public ResponseEntity<ApiResponse<UserDto>> lockUser(@PathVariable String id) {
        UserDto user = adminService.lockUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, MessageConstant.ACCOUNT_LOCKED_SUCCESS));
    }

    @PutMapping("/users/{id}/unlock")
    public ResponseEntity<ApiResponse<UserDto>> unlockUser(@PathVariable String id) {
        UserDto user = adminService.unlockUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, MessageConstant.ACCOUNT_UNLOCKED_SUCCESS));
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (id.equals(principal.getId())) {
            throw ApiException.badRequest(MessageConstant.CANNOT_DELETE_YOURSELF);
        }
        adminService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.ACCOUNT_DELETED_SUCCESS));
    }

    @PutMapping("/users/{id}/restore")
    public ResponseEntity<ApiResponse<UserDto>> restoreUser(@PathVariable String id) {
        UserDto user = adminService.restoreUser(id);
        return ResponseEntity.ok(ApiResponse.success(user, MessageConstant.ACCOUNT_RESTORED_SUCCESS));
    }

    @DeleteMapping("/users/{id}/permanent")
    public ResponseEntity<ApiResponse<Void>> permanentlyDeleteUser(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        if (id.equals(principal.getId())) {
            throw ApiException.badRequest(MessageConstant.CANNOT_DELETE_YOURSELF_PERMANENTLY);
        }
        adminService.permanentlyDeleteUser(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.ACCOUNT_PERMANENTLY_DELETED_SUCCESS));
    }

    // ==================== ACTIVITY LOGS ====================

    @GetMapping("/activity-logs")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActivityLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return ResponseEntity.ok(ApiResponse.success(
                adminService.getActivityLogsPaginated(page, size, type, userId, startDate, endDate)));
    }

    // ==================== SETTINGS ====================

    @GetMapping("/settings")
    public ResponseEntity<ApiResponse<List<SystemSettingDto>>> getAllSettings() {
        List<SystemSettingDto> settings = adminService.getAllSettings();
        return ResponseEntity.ok(ApiResponse.success(settings));
    }

    @GetMapping("/settings/{key}")
    public ResponseEntity<ApiResponse<SystemSettingDto>> getSetting(@PathVariable String key) {
        SystemSettingDto setting = adminService.getSettingByKey(key);
        return ResponseEntity.ok(ApiResponse.success(setting));
    }

    @PostMapping("/settings")
    public ResponseEntity<ApiResponse<SystemSettingDto>> createSetting(@RequestBody CreateSettingRequest request) {
        SystemSettingDto setting = adminService.createSetting(request);
        return ResponseEntity.ok(ApiResponse.success(setting, MessageConstant.SETTING_CREATED));
    }

    @PutMapping("/settings/{key}")
    public ResponseEntity<ApiResponse<SystemSettingDto>> updateSetting(
            @PathVariable String key,
            @RequestBody UpdateSettingRequest request) {
        SystemSettingDto setting = adminService.updateSetting(key, request);
        return ResponseEntity.ok(ApiResponse.success(setting, MessageConstant.SETTING_UPDATED));
    }

    @DeleteMapping("/settings/{key}")
    public ResponseEntity<ApiResponse<Void>> deleteSetting(@PathVariable String key) {
        adminService.deleteSetting(key);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.SETTING_DELETED));
    }

    @PostMapping("/settings/bulk")
    public ResponseEntity<ApiResponse<List<SystemSettingDto>>> updateBulkSettings(
            @RequestBody Map<String, String> settings) {
        List<SystemSettingDto> dtos = adminService.updateBulkSettings(settings);
        return ResponseEntity.ok(ApiResponse.success(dtos, String.format(MessageConstant.BULK_SETTINGS_UPDATED, dtos.size())));
    }

    // ==================== ANNOUNCEMENTS ====================

    @GetMapping("/announcements")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAllAnnouncements(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        Map<String, Object> response = adminService.getAllAnnouncementsPaginated(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<AnnouncementDto>> getAnnouncement(@PathVariable String id) {
        AnnouncementDto announcement = adminService.getAnnouncementById(id);
        return ResponseEntity.ok(ApiResponse.success(announcement));
    }

    @PostMapping("/announcements")
    public ResponseEntity<ApiResponse<AnnouncementDto>> createAnnouncement(
            @RequestBody CreateAnnouncementRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AnnouncementDto announcement = adminService.createAnnouncement(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(announcement, MessageConstant.ANNOUNCEMENT_CREATED));
    }

    @PutMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<AnnouncementDto>> updateAnnouncement(
            @PathVariable String id,
            @RequestBody UpdateAnnouncementRequest request) {
        AnnouncementDto announcement = adminService.updateAnnouncement(id, request);
        return ResponseEntity.ok(ApiResponse.success(announcement, MessageConstant.ANNOUNCEMENT_UPDATED));
    }

    @PutMapping("/announcements/{id}/publish")
    public ResponseEntity<ApiResponse<AnnouncementDto>> publishAnnouncement(@PathVariable String id) {
        AnnouncementDto announcement = adminService.publishAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(announcement, MessageConstant.ANNOUNCEMENT_PUBLISHED));
    }

    @PutMapping("/announcements/{id}/archive")
    public ResponseEntity<ApiResponse<AnnouncementDto>> archiveAnnouncement(@PathVariable String id) {
        AnnouncementDto announcement = adminService.archiveAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(announcement, MessageConstant.ANNOUNCEMENT_ARCHIVED));
    }

    @DeleteMapping("/announcements/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAnnouncement(@PathVariable String id) {
        adminService.deleteAnnouncement(id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.ANNOUNCEMENT_DELETED));
    }

    // ==================== NOTIFICATIONS ====================

    @PostMapping("/notifications/send")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendNotification(
            @Valid @RequestBody SendNotificationRequest request) {
        int count = adminService.sendNotificationToUsers(request);
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("sentCount", count),
                String.format("Đã gửi thông báo đến %d người dùng", count)));
    }

    @PostMapping("/notifications/send/{userId}")
    public ResponseEntity<ApiResponse<Void>> sendNotificationToUser(
            @PathVariable String userId,
            @Valid @RequestBody SendNotificationRequest request) {
        adminService.sendNotificationToUser(userId, request.title(), request.message());
        return ResponseEntity.ok(ApiResponse.success(null, "Đã gửi thông báo"));
    }
}
