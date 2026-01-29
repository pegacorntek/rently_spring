package com.pegacorn.rently.constant;

/**
 * Centralized error and success messages for the application.
 * All user-facing messages should be defined here for easy management.
 */
public final class Messages {

    private Messages() {}

    // ==================== Common ====================
    public static final String ACCESS_DENIED = "Không có quyền truy cập";

    // ==================== House ====================
    public static final String HOUSE_NOT_FOUND = "Nhà không tồn tại";

    // ==================== Room ====================
    public static final String ROOM_NOT_FOUND = "Phòng không tồn tại";
    public static final String ROOM_CODE_EXISTS = "Tên phòng đã tồn tại trong nhà này";
    public static final String ROOM_CANNOT_DELETE_RENTED = "Không thể xóa phòng đang có người thuê";

    // ==================== Tenant ====================
    public static final String TENANT_ALREADY_IN_ROOM = "Người này đã ở trong phòng rồi";
    public static final String TENANT_NOT_FOUND_IN_ROOM = "Không tìm thấy người thuê trong phòng này";
    public static final String TENANT_ID_NUMBER_REQUIRED = "Vui lòng nhập số CCCD/CMND";
    public static final String TENANT_FULLNAME_REQUIRED = "Vui lòng nhập họ tên";
    public static final String TENANT_ID_NUMBER_EXISTS = "Số CCCD/CMND đã được đăng ký";
    public static final String TENANT_ID_NUMBER_BELONGS_TO_OTHER = "Số CCCD/CMND đã được sử dụng";
    public static final String CANNOT_REMOVE_CONTRACT_TENANT = "Không thể xóa người thuê chính khi hợp đồng còn hiệu lực. Vui lòng kết thúc hợp đồng trước.";

    // ==================== Contract ====================
    public static final String CONTRACT_NOT_FOUND = "Hợp đồng không tồn tại";

    // ==================== Invoice ====================
    public static final String INVOICE_NOT_FOUND = "Hóa đơn không tồn tại";

    // ==================== User ====================
    public static final String USER_NOT_FOUND = "Tài khoản không tồn tại";
    public static final String USER_PHONE_EXISTS = "Số điện thoại đã được sử dụng";
    public static final String USER_EMAIL_EXISTS = "Email đã được sử dụng";

    // ==================== Auth ====================
    public static final String AUTH_INVALID_CREDENTIALS = "Số điện thoại hoặc mật khẩu không đúng";
    public static final String AUTH_ACCOUNT_DISABLED = "Tài khoản đã bị vô hiệu hóa";
}
