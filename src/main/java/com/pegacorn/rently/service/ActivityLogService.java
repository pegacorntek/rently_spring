package com.pegacorn.rently.service;

import com.pegacorn.rently.dto.activity.ActivityLogDto;
import com.pegacorn.rently.entity.ActivityLog;
import com.pegacorn.rently.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository activityLogRepository;

    public List<ActivityLogDto> getRecentActivities(String landlordId, int limit) {
        return activityLogRepository
                .findByLandlordIdOrderByCreatedAtDesc(landlordId, PageRequest.of(0, limit))
                .stream()
                .map(ActivityLogDto::fromEntity)
                .toList();
    }

    public void log(String landlordId, ActivityLog.ActivityType type, String entityId,
                    String entityType, String description, String metadata) {
        ActivityLog activity = ActivityLog.builder()
                .id(UUID.randomUUID().toString())
                .landlordId(landlordId)
                .type(type)
                .entityId(entityId)
                .entityType(entityType)
                .description(description)
                .metadata(metadata)
                .createdAt(LocalDateTime.now())
                .build();
        activityLogRepository.save(activity);
    }

    public void logInvoiceCreated(String landlordId, String invoiceId, String roomCode, String tenantName, String amount) {
        log(landlordId, ActivityLog.ActivityType.INVOICE_CREATED, invoiceId, "INVOICE",
                "Tạo hóa đơn cho phòng " + roomCode + " - " + tenantName,
                "{\"amount\": \"" + amount + "\", \"roomCode\": \"" + roomCode + "\"}");
    }

    public void logInvoiceSent(String landlordId, String invoiceId, String roomCode, String tenantName) {
        log(landlordId, ActivityLog.ActivityType.INVOICE_SENT, invoiceId, "INVOICE",
                "Gửi hóa đơn cho " + tenantName + " (phòng " + roomCode + ")",
                "{\"roomCode\": \"" + roomCode + "\"}");
    }

    public void logInvoicePaid(String landlordId, String invoiceId, String roomCode, String tenantName, String amount) {
        log(landlordId, ActivityLog.ActivityType.INVOICE_PAID, invoiceId, "INVOICE",
                tenantName + " đã thanh toán hóa đơn phòng " + roomCode,
                "{\"amount\": \"" + amount + "\", \"roomCode\": \"" + roomCode + "\"}");
    }

    public void logInvoiceCancelled(String landlordId, String invoiceId, String roomCode) {
        log(landlordId, ActivityLog.ActivityType.INVOICE_CANCELLED, invoiceId, "INVOICE",
                "Hủy hóa đơn phòng " + roomCode,
                "{\"roomCode\": \"" + roomCode + "\"}");
    }

    public void logContractCreated(String landlordId, String contractId, String roomCode, String tenantName) {
        log(landlordId, ActivityLog.ActivityType.CONTRACT_CREATED, contractId, "CONTRACT",
                "Tạo hợp đồng mới cho phòng " + roomCode + " - " + tenantName,
                "{\"roomCode\": \"" + roomCode + "\"}");
    }

    public void logContractSigned(String landlordId, String contractId, String roomCode, String tenantName) {
        log(landlordId, ActivityLog.ActivityType.CONTRACT_SIGNED, contractId, "CONTRACT",
                "Hợp đồng phòng " + roomCode + " đã được ký - " + tenantName,
                "{\"roomCode\": \"" + roomCode + "\"}");
    }

    public void logContractEnded(String landlordId, String contractId, String roomCode, String tenantName) {
        log(landlordId, ActivityLog.ActivityType.CONTRACT_ENDED, contractId, "CONTRACT",
                "Kết thúc hợp đồng phòng " + roomCode + " - " + tenantName,
                "{\"roomCode\": \"" + roomCode + "\"}");
    }

    public void logDepositConfirmed(String landlordId, String contractId, String roomCode, String tenantName, String amount) {
        log(landlordId, ActivityLog.ActivityType.DEPOSIT_CONFIRMED, contractId, "CONTRACT",
                "Xác nhận tiền cọc phòng " + roomCode + " - " + tenantName,
                "{\"amount\": \"" + amount + "\", \"roomCode\": \"" + roomCode + "\"}");
    }

    public void logRoomCreated(String landlordId, String roomId, String roomCode, String houseName) {
        log(landlordId, ActivityLog.ActivityType.ROOM_CREATED, roomId, "ROOM",
                "Thêm phòng " + roomCode + " vào " + houseName,
                "{\"roomCode\": \"" + roomCode + "\", \"houseName\": \"" + houseName + "\"}");
    }

    public void logRoomStatusChanged(String landlordId, String roomId, String roomCode, String oldStatus, String newStatus) {
        log(landlordId, ActivityLog.ActivityType.ROOM_STATUS_CHANGED, roomId, "ROOM",
                "Phòng " + roomCode + " chuyển trạng thái: " + oldStatus + " → " + newStatus,
                "{\"roomCode\": \"" + roomCode + "\", \"oldStatus\": \"" + oldStatus + "\", \"newStatus\": \"" + newStatus + "\"}");
    }

    public void logHouseCreated(String landlordId, String houseId, String houseName) {
        log(landlordId, ActivityLog.ActivityType.HOUSE_CREATED, houseId, "HOUSE",
                "Tạo nhà trọ mới: " + houseName,
                "{\"houseName\": \"" + houseName + "\"}");
    }

    public void logMeterReadingSaved(String landlordId, String roomId, String roomCode, String periodMonth) {
        log(landlordId, ActivityLog.ActivityType.METER_READING_SAVED, roomId, "ROOM",
                "Ghi chỉ số điện nước phòng " + roomCode + " tháng " + periodMonth,
                "{\"roomCode\": \"" + roomCode + "\", \"periodMonth\": \"" + periodMonth + "\"}");
    }

    public void logTenantAdded(String landlordId, String roomId, String roomCode, String tenantName, String tenantPhone) {
        log(landlordId, ActivityLog.ActivityType.TENANT_ADDED, roomId, "ROOM",
                "Thêm khách " + tenantName + " vào phòng " + roomCode,
                "{\"roomCode\": \"" + roomCode + "\", \"tenantName\": \"" + tenantName + "\", \"tenantPhone\": \"" + tenantPhone + "\"}");
    }

    public void logUserLogin(String userId, String phone) {
        log(userId, ActivityLog.ActivityType.USER_LOGIN, userId, "USER",
                "Đăng nhập hệ thống",
                "{\"phone\": \"" + phone + "\"}");
    }

    public void logUserLogout(String userId) {
        log(userId, ActivityLog.ActivityType.USER_LOGOUT, userId, "USER",
                "Đăng xuất hệ thống",
                null);
    }
}
