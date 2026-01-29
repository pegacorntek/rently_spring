package com.pegacorn.rently.service;

public interface SmsService {
    /**
     * Send SMS message to a phone number
     * @param phone The phone number (with or without country code)
     * @param message The message content
     * @return true if sent successfully, false otherwise
     */
    boolean sendSms(String phone, String message);

    /**
     * Send OTP code to a phone number
     * @param phone The phone number
     * @param otpCode The OTP code
     * @return true if sent successfully, false otherwise
     */
    default boolean sendOtp(String phone, String otpCode) {
        String message = "Ma xac thuc Rently cua ban la: " + otpCode + ". Ma co hieu luc trong 5 phut.";
        return sendSms(phone, message);
    }

    /**
     * Send invoice notification to tenant
     * @param phone The tenant's phone number
     * @param roomCode The room code
     * @param houseName The house name
     * @param houseAddress The house address
     * @param periodMonth The billing period (e.g., "2024-01")
     * @param amount The formatted amount string
     * @param dueDate The due date string
     * @param invoiceUrl The public invoice URL
     * @return true if sent successfully, false otherwise
     */
    default boolean sendInvoiceNotification(String phone, String roomCode, String houseName, String houseAddress, String periodMonth, String amount, String dueDate, String invoiceUrl) {
        String message = String.format(
            "[Rently] Hoa don phong %s - %s (%s), thang %s: %s. Han TT: %s. Xem chi tiet: %s",
            roomCode, houseName, houseAddress, periodMonth, amount, dueDate, invoiceUrl
        );
        return sendSms(phone, message);
    }

    /**
     * Send welcome SMS to newly added tenant
     * @param phone The tenant's phone number
     * @param tenantName The tenant's full name
     * @param roomCode The room code
     * @param houseName The house name
     * @param loginUrl The login URL
     * @param password The initial password (ID number)
     * @return true if sent successfully, false otherwise
     */
    default boolean sendTenantWelcome(String phone, String tenantName, String roomCode, String houseName, String loginUrl, String password) {
        String message = String.format(
            "[Rently] Xin chao %s! Ban da duoc them vao phong %s - %s. Dang nhap tai: %s. SĐT: %s, Mat khau: %s",
            tenantName, roomCode, houseName, loginUrl, phone, password
        );
        return sendSms(phone, message);
    }
}
