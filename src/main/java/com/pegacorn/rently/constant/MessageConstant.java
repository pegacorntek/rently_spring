package com.pegacorn.rently.constant;

public class MessageConstant {

    // Auth Success
    public static final String OTP_SENT_SUCCESS = "Gửi mã OTP thành công";
    public static final String OTP_VERIFIED_SUCCESS = "Xác thực OTP thành công";
    public static final String REGISTER_SUCCESS = "Đăng ký thành công";
    public static final String LOGIN_SUCCESS = "Đăng nhập thành công";
    public static final String PASSWORD_RESET_SUCCESS = "Đặt lại mật khẩu thành công";
    public static final String TOKEN_REFRESH_SUCCESS = "Làm mới token thành công";
    public static final String CHANGE_PASSWORD_SUCCESS = "Đổi mật khẩu thành công";
    public static final String PROFILE_UPDATED_SUCCESS = "Cập nhật hồ sơ thành công";
    public static final String BANK_SETTINGS_UPDATED_SUCCESS = "Cập nhật thông tin ngân hàng thành công";

    // Auth Errors
    public static final String PHONE_ALREADY_EXISTS = "Số điện thoại đã được đăng ký";
    public static final String PHONE_NOT_FOUND = "Số điện thoại không tồn tại";
    public static final String USER_NOT_FOUND = "Người dùng không tồn tại";
    public static final String OTP_INVALID_OR_EXPIRED = "Mã OTP không hợp lệ hoặc đã hết hạn";
    public static final String OTP_INVALID = "Mã OTP không hợp lệ";
    public static final String OTP_REQUIRED_FIRST = "Vui lòng xác thực OTP trước";
    public static final String PASSWORD_MISMATCH = "Mật khẩu không khớp";
    public static final String RETYPE_PASSWORD_MISMATCH = "Mật khẩu nhập lại không khớp";
    public static final String CURRENT_PASSWORD_INCORRECT = "Mật khẩu hiện tại không đúng";
    public static final String CREDENTIALS_INVALID = "Thông tin đăng nhập không chính xác";
    public static final String ACCOUNT_LOCKED = "Tài khoản đã bị khóa";
    public static final String REFRESH_TOKEN_EXPIRED = "Refresh token đã hết hạn";
    public static final String ID_NUMBER_CONFLICT = "Số CCCD/CMND này đã được sử dụng bởi tài khoản khác";
    public static final String ACCESS_DENIED = "Không có quyền truy cập";
    public static final String NOT_AUTHORIZED = "Không được phép";

    // Admin / User Management
    public static final String ACCOUNT_LOCKED_SUCCESS = "Đã khóa tài khoản";
    public static final String ACCOUNT_UNLOCKED_SUCCESS = "Đã mở khóa tài khoản";
    public static final String ACCOUNT_DELETED_SUCCESS = "Đã xóa tài khoản";
    public static final String ACCOUNT_RESTORED_SUCCESS = "Đã khôi phục tài khoản";
    public static final String ACCOUNT_PERMANENTLY_DELETED_SUCCESS = "Đã xóa vĩnh viễn tài khoản";
    public static final String CANNOT_DELETE_YOURSELF = "Không thể xóa chính tài khoản của bạn";
    public static final String CANNOT_DELETE_YOURSELF_PERMANENTLY = "Không thể xóa vĩnh viễn chính tài khoản của bạn";
    public static final String CANNOT_LOCK_ADMIN = "Không thể khóa tài khoản quản trị viên hệ thống";
    public static final String CANNOT_DELETE_ADMIN = "Không thể xóa tài khoản quản trị viên hệ thống";
    public static final String USER_NOT_DELETED = "Người dùng chưa bị xóa";

    // Settings
    public static final String SETTING_CREATED = "Đã tạo cài đặt";
    public static final String SETTING_UPDATED = "Đã cập nhật cài đặt";
    public static final String SETTING_DELETED = "Đã xóa cài đặt";
    public static final String SETTING_NOT_FOUND = "Không tìm thấy cài đặt: ";
    public static final String SETTING_ALREADY_EXISTS = "Cài đặt đã tồn tại: ";
    public static final String BULK_SETTINGS_UPDATED = "Đã cập nhật %d cài đặt";

    // Announcements
    public static final String ANNOUNCEMENT_CREATED = "Đã tạo thông báo";
    public static final String ANNOUNCEMENT_UPDATED = "Đã cập nhật thông báo";
    public static final String ANNOUNCEMENT_PUBLISHED = "Đã xuất bản thông báo";
    public static final String ANNOUNCEMENT_ARCHIVED = "Đã lưu trữ thông báo";
    public static final String ANNOUNCEMENT_DELETED = "Đã xóa thông báo";
    public static final String ANNOUNCEMENT_NOT_FOUND = "Không tìm thấy thông báo";

    // AI
    public static final String CHAT_RESPONSE_GENERATED = "Phản hồi chat đã được tạo";

    // Push Notifications
    public static final String PUSH_SUBSCRIBED_SUCCESS = "Đăng ký nhận thông báo thành công";
    public static final String PUSH_UNSUBSCRIBED_SUCCESS = "Hủy đăng ký nhận thông báo thành công";
    public static final String PUSH_SENT_SUCCESS = "Đã gửi thông báo";
    public static final String PUSH_TEST_SENT_SUCCESS = "Đã gửi thông báo thử nghiệm";

    // Payment
    public static final String PROOF_UPLOADED_SUCCESS = "Tải lên minh chứng thành công";
    public static final String PAYMENT_RECORDED_SUCCESS = "Ghi nhận thanh toán thành công";
    public static final String PAYMENT_CONFIRMED_SUCCESS = "Xác nhận thanh toán thành công";
    public static final String CASH_PAYMENT_RECORDED_SUCCESS = "Ghi nhận thanh toán tiền mặt thành công";
    public static final String PAYMENT_NOT_FOUND = "Không tìm thấy thanh toán";
    public static final String PAYMENT_NOT_PENDING = "Thanh toán không ở trạng thái chờ xử lý";
    public static final String INVOICE_NOT_PAYABLE = "Hóa đơn không thể thanh toán";
    public static final String CANNOT_ADD_PAYMENT_TO_INVOICE = "Không thể thêm thanh toán vào hóa đơn này";

    // Ticket
    public static final String TICKET_CREATED_SUCCESS = "Tạo yêu cầu hỗ trợ thành công";
    public static final String TICKET_UPDATED_SUCCESS = "Cập nhật yêu cầu hỗ trợ thành công";
    public static final String TICKET_NOT_FOUND = "Không tìm thấy yêu cầu hỗ trợ";
    public static final String NOT_TENANT_OF_ROOM = "Bạn không phải là người thuê của phòng này";

    // Privacy
    public static final String PRIVACY_UPDATED_SUCCESS = "Cập nhật cài đặt quyền riêng tư thành công";

    // House
    public static final String HOUSE_CREATED_SUCCESS = "Tạo nhà trọ thành công";
    public static final String HOUSE_UPDATED_SUCCESS = "Cập nhật nhà trọ thành công";
    public static final String HOUSE_DELETED_SUCCESS = "Xóa nhà trọ thành công";
    public static final String HOUSE_NOT_FOUND = "Không tìm thấy nhà trọ";
    public static final String HOUSE_ID_REQUIRED = "Yêu cầu ID nhà trọ";

    // Room
    public static final String ROOM_CREATED_SUCCESS = "Tạo phòng thành công";
    public static final String ROOM_UPDATED_SUCCESS = "Cập nhật phòng thành công";
    public static final String ROOM_DELETED_SUCCESS = "Xóa phòng thành công";
    public static final String TENANT_ADDED_SUCCESS = "Thêm người thuê thành công";
    public static final String TENANT_REMOVED_SUCCESS = "Xóa người thuê thành công";
    public static final String PRIMARY_TENANT_UPDATED_SUCCESS = "Cập nhật người thuê chính thành công";
    public static final String ROOM_NOT_FOUND = "Không tìm thấy phòng";
    public static final String CANNOT_MAINTENANCE_WITH_CONTRACT = "Không thể chuyển sang bảo trì khi phòng đang có hợp đồng";
    public static final String ROOM_CODE_EXISTS_IN_HOUSE = "Mã phòng đã tồn tại trong nhà này";
    public static final String ROOM_CODE_EXISTS = "Mã phòng đã tồn tại";
    public static final String CANNOT_DELETE_RENTED_ROOM = "Không thể xóa phòng đang có người thuê";
    public static final String TENANT_ALREADY_IN_ROOM = "Người thuê đã có trong phòng này";
    public static final String ID_NUMBER_REQUIRED = "Yêu cầu số CCCD/CMND";
    public static final String FULL_NAME_REQUIRED = "Yêu cầu họ tên";
    public static final String CANNOT_REMOVE_CONTRACT_TENANT = "Không thể xóa người thuê đang đứng tên hợp đồng";
    public static final String TENANT_NOT_FOUND_IN_ROOM = "Người thuê không có trong phòng này";

    // Contract
    public static final String CONTRACT_CREATED_SUCCESS = "Tạo hợp đồng thành công";
    public static final String CONTRACT_UPDATED_SUCCESS = "Cập nhật hợp đồng thành công";
    public static final String CONTRACT_ACTIVATED_SUCCESS = "Kích hoạt hợp đồng thành công";
    public static final String CONTRACT_ENDED_SUCCESS = "Kết thúc hợp đồng thành công";
    public static final String DEPOSIT_CONFIRMED_SUCCESS = "Xác nhận tiền cọc thành công";
    public static final String CONTRACT_SNAPSHOT_REFRESHED_SUCCESS = "Làm mới bản ghi hợp đồng thành công";
    public static final String CONTRACT_NOT_FOUND = "Không tìm thấy hợp đồng";
    public static final String ROOM_HAS_ACTIVE_CONTRACT = "Phòng đã có hợp đồng đang hoạt động";
    public static final String TENANT_NOT_FOUND_WITH_PHONE = "Không tìm thấy người thuê với số điện thoại này";
    public static final String LANDLORD_NOT_FOUND = "Không tìm thấy chủ nhà";
    public static final String CANNOT_UPDATE_ENDED_CONTRACT = "Không thể cập nhật hợp đồng đã kết thúc";
    public static final String ONLY_DRAFT_CAN_ACTIVATE = "Chỉ hợp đồng nháp mới có thể kích hoạt";
    public static final String ONLY_ACTIVE_CAN_REFRESH = "Chỉ hợp đồng đang hoạt động mới có thể làm mới bản ghi";
    public static final String ONLY_ACTIVE_OR_DRAFT_CAN_END = "Chỉ hợp đồng đang hoạt động hoặc nháp mới có thể kết thúc/xóa";
    public static final String DEPOSIT_ALREADY_CONFIRMED = "Tiền cọc đã được xác nhận";
    public static final String CONTRACT_NOT_ACTIVE = "Hợp đồng chưa được kích hoạt";
    public static final String CONTRACT_DELETED_SUCCESS = "Xóa hợp đồng nháp thành công";
    public static final String ONLY_DRAFT_CAN_DELETE = "Chỉ có thể xóa hợp đồng nháp";

    // Contract Template
    public static final String CONTRACT_TEMPLATE_CREATED_SUCCESS = "Đã tạo mẫu hợp đồng";
    public static final String CONTRACT_TEMPLATE_UPDATED_SUCCESS = "Đã cập nhật mẫu hợp đồng";
    public static final String CONTRACT_TEMPLATE_DELETED_SUCCESS = "Đã xóa mẫu hợp đồng";
    public static final String CONTRACT_TEMPLATE_SET_DEFAULT_SUCCESS = "Đã đặt làm mẫu mặc định";
    public static final String TEMPLATE_NOT_FOUND = "Mẫu hợp đồng không tồn tại";
    public static final String NO_ACCESS_TO_HOUSE = "Không có quyền truy cập nhà này";

    // Invoice
    public static final String INVOICE_GENERATED_SUCCESS = "Tạo hóa đơn thành công";
    public static final String INVOICE_SENT_SUCCESS = "Gửi hóa đơn thành công";
    public static final String SMS_SENT_SUCCESS = "Gửi SMS thành công";
    public static final String INVOICE_CANCELLED_SUCCESS = "Hủy hóa đơn thành công";
    public static final String METER_READING_SAVED_SUCCESS = "Lưu chỉ số điện nước thành công";
    public static final String SHORTFALL_FLAGGED_SUCCESS = "Đã ghi nhận thiếu hụt cho tháng sau";
    public static final String INVOICE_DELETED_SUCCESS = "Xóa hóa đơn thành công";
    public static final String SHORTFALL_MARKED_APPLIED_SUCCESS = "Đã đánh dấu thiếu hụt là đã áp dụng";
    public static final String SHORTFALL_REMOVED_SUCCESS = "Đã xóa ghi nhận thiếu hụt";
    public static final String INVOICE_NOT_FOUND = "Không tìm thấy hóa đơn";
    public static final String INVOICE_ALREADY_EXISTS = "Hóa đơn đã tồn tại cho hợp đồng và kỳ này";
    public static final String ONLY_DRAFT_INVOICE_CAN_SEND = "Chỉ hóa đơn nháp mới có thể gửi";
    public static final String NO_PRIMARY_TENANT = "Không tìm thấy người thuê chính cho phòng này";
    public static final String PRIMARY_TENANT_NO_PHONE = "Người thuê chính không có số điện thoại";
    public static final String FAILED_TO_SEND_SMS = "Gửi SMS thất bại";
    public static final String CANNOT_CANCEL_PAID_INVOICE = "Không thể hủy hóa đơn đã thanh toán";
    public static final String CANNOT_GENERATE_QR = "Không thể tạo mã QR cho trạng thái hóa đơn này";
    public static final String BANK_INFO_NOT_CONFIGURED = "Chưa cấu hình thông tin ngân hàng";
    public static final String SHORTFALL_ALREADY_FLAGGED = "Đã ghi nhận thiếu hụt cho kỳ này";
    public static final String NO_SHORTFALL_TO_FLAG = "Không có thiếu hụt để ghi nhận";
    public static final String SHORTFALL_NOT_FOUND = "Không tìm thấy ghi nhận thiếu hụt";
    public static final String ONLY_DELETE_PENDING_SHORTFALL = "Chỉ có thể xóa các ghi nhận thiếu hụt đang chờ xử lý";
    public static final String NO_SHORTFALL_TO_APPLY = "Không có thiếu hụt để áp dụng";

    // Service Fee
    public static final String SERVICE_FEE_CREATED_SUCCESS = "Tạo phí dịch vụ thành công";
    public static final String SERVICE_FEE_UPDATED_SUCCESS = "Cập nhật phí dịch vụ thành công";
    public static final String SERVICE_FEE_DELETED_SUCCESS = "Xóa phí dịch vụ thành công";
    public static final String SERVICE_FEE_NOT_FOUND = "Không tìm thấy phí dịch vụ";
    public static final String SERVICE_FEE_EXISTS = "Phí dịch vụ đã tồn tại trong nhà này";

    // Amenity
    public static final String AMENITY_CREATED_SUCCESS = "Tạo tiện ích thành công";
    public static final String AMENITY_UPDATED_SUCCESS = "Cập nhật tiện ích thành công";
    public static final String AMENITY_DELETED_SUCCESS = "Xóa tiện ích thành công";
    public static final String AMENITY_ADDED_TO_ROOM_SUCCESS = "Thêm tiện ích vào phòng thành công";
    public static final String ROOM_AMENITY_UPDATED_SUCCESS = "Cập nhật tiện ích phòng thành công";
    public static final String AMENITY_REMOVED_FROM_ROOM_SUCCESS = "Xóa tiện ích khỏi phòng thành công";
    public static final String SHARED_AMENITY_ADDED_SUCCESS = "Thêm tiện ích chung thành công";
    public static final String SHARED_AMENITY_REMOVED_SUCCESS = "Xóa tiện ích chung thành công";
    public static final String SHARED_AMENITIES_UPDATED_SUCCESS = "Cập nhật tiện ích chung thành công";
    public static final String AMENITY_NOT_FOUND = "Không tìm thấy tiện ích";
    public static final String AMENITY_ALREADY_ADDED = "Tiện ích đã được thêm vào phòng này";
    public static final String ROOM_AMENITY_NOT_FOUND = "Không tìm thấy tiện ích phòng";
    public static final String AMENITY_ALREADY_SHARED = "Tiện ích đã được thêm làm tiện ích chung";

    // Expense
    public static final String EXPENSE_CREATED_SUCCESS = "Tạo khoản chi thành công";
    public static final String EXPENSE_UPDATED_SUCCESS = "Cập nhật khoản chi thành công";
    public static final String EXPENSE_DELETED_SUCCESS = "Xóa khoản chi thành công";
    public static final String EXPENSE_MARKED_PAID_SUCCESS = "Đã đánh dấu thanh toán";
    public static final String EXPENSE_NOT_FOUND = "Không tìm thấy khoản chi";
    public static final String EXPENSE_ALREADY_PAID = "Khoản chi này đã được thanh toán";
    public static final String FAILED_TO_SAVE_FILE = "Không thể lưu file: ";

    // Expense Category
    public static final String EXPENSE_CATEGORY_CREATED_SUCCESS = "Tạo danh mục thành công";
    public static final String EXPENSE_CATEGORY_UPDATED_SUCCESS = "Cập nhật danh mục thành công";
    public static final String EXPENSE_CATEGORY_DELETED_SUCCESS = "Xóa danh mục thành công";
    public static final String HOUSE_EXPENSE_CATEGORIES_UPDATED_SUCCESS = "Cập nhật danh mục cho nhà thành công";
    public static final String EXPENSE_CATEGORY_EXISTS = "Danh mục với tên này đã tồn tại";
    public static final String EXPENSE_CATEGORY_NOT_FOUND = "Danh mục không tồn tại";
    public static final String CANNOT_DELETE_USED_CATEGORY = "Không thể xóa danh mục đang được sử dụng bởi các khoản chi";
    public static final String CATEGORY_NOT_OWNED = "Danh mục không thuộc về bạn";

    // OCR
    public static final String CONTRACT_EXTRACTED_SUCCESS = "Đã trích xuất nội dung hợp đồng";
    public static final String CONTRACT_DATA_EXTRACTED_SUCCESS = "Đã trích xuất dữ liệu hợp đồng";
    public static final String UPLOAD_AT_LEAST_ONE_IMAGE = "Vui lòng tải lên ít nhất 1 hình ảnh";
    public static final String MAX_10_IMAGES = "Tối đa 10 hình ảnh";
    public static final String FILE_NOT_IMAGE = "File không phải là hình ảnh: ";
    public static final String FILE_TOO_LARGE = "File quá lớn (tối đa 10MB): ";
    public static final String NO_VALID_IMAGES = "Không có hình ảnh hợp lệ";

    // Task
    public static final String TASK_CREATED_SUCCESS = "Đã tạo việc cần làm";
    public static final String TASK_DELETED_SUCCESS = "Đã xóa việc cần làm";
    public static final String TASK_NOT_FOUND = "Không tìm thấy việc cần làm";

    // SePay
    public static final String SEPAY_WEBHOOK_OK = "OK";
    public static final String SEPAY_WEBHOOK_RECEIVED = "Received";
    public static final String INVALID_API_KEY = "API Key không hợp lệ";

    // Tenant
    public static final String TENANT_NOT_FOUND = "Không tìm thấy người thuê";

    private MessageConstant() {
        // Private constructor to prevent instantiation
    }
}
