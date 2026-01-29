package com.pegacorn.rently.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pegacorn.rently.dto.ocr.ExtractContractDataResponse;
import com.pegacorn.rently.dto.ocr.ScanContractResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OcrService {

    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;

    private static final String SYSTEM_PROMPT = """
            Bạn là chuyên gia trích xuất nội dung hợp đồng thuê phòng/nhà trọ Việt Nam.

            NGUYÊN TẮC QUAN TRỌNG:
            - KHÔNG thay đổi, sửa đổi, hoặc viết lại bất kỳ nội dung nào
            - GIỮ NGUYÊN 100% văn bản gốc từ hợp đồng
            - GIỮ NGUYÊN cấu trúc, thứ tự điều khoản, định dạng
            - Chỉ chuyển đổi sang HTML, không thêm/bớt nội dung

            NHIỆM VỤ:
            Trích xuất CHÍNH XÁC nội dung từ hình ảnh hợp đồng thành HTML.
            Các trang được sắp xếp theo thứ tự từ trang 1 đến trang cuối.

            YÊU CẦU HTML:
            1. Dùng <h1> cho tiêu đề chính, <h2>/<h3> cho tiêu đề phụ
            2. Dùng <p> cho đoạn văn
            3. Dùng <table> cho bảng biểu
            4. Dùng <ul>/<ol> cho danh sách

            PLACEHOLDER - CHỈ THAY THẾ:
            Chỗ trống (___), chỗ điền tay, hoặc dữ liệu cụ thể (tên, số điện thoại, địa chỉ, số tiền...)
            KHÔNG thay thế văn bản mô tả hoặc điều khoản.

            VÍ DỤ ĐÚNG:
            "Tiền thuê: 3.000.000 đồng/tháng" → "Tiền thuê: <span data-placeholder-key="tien_thue_thang" data-placeholder-label="Tiền thuê/tháng" class="placeholder-node">tien_thue_thang</span> đồng/tháng"
            "Ông/Bà: Nguyễn Văn A" → "Ông/Bà: <span data-placeholder-key="ten_chu_nha" data-placeholder-label="Tên chủ nhà" class="placeholder-node">ten_chu_nha</span>"

            CÁC PLACEHOLDER:
            - ten_chu_nha (Tên chủ nhà)
            - sdt_chu_nha (SĐT chủ nhà)
            - cccd_chu_nha (CCCD chủ nhà)
            - dia_chi_chu_nha (Địa chỉ chủ nhà)
            - ten_nguoi_thue (Tên người thuê)
            - sdt_nguoi_thue (SĐT người thuê)
            - cccd_nguoi_thue (CCCD người thuê)
            - ngay_sinh_nguoi_thue (Ngày sinh người thuê)
            - que_quan_nguoi_thue (Quê quán người thuê)
            - ma_phong (Mã phòng)
            - tang (Tầng)
            - dien_tich (Diện tích m²)
            - dia_chi_nha_tro (Địa chỉ nhà trọ)
            - ten_nha_tro (Tên nhà trọ)
            - tien_thue_thang (Tiền thuê/tháng)
            - tien_coc (Tiền cọc)
            - so_thang_coc (Số tháng cọc)
            - ngay_bat_dau (Ngày bắt đầu)
            - ngay_ket_thuc (Ngày kết thúc)
            - thoi_han (Thời hạn)
            - ky_thanh_toan (Kỳ thanh toán)
            - ngay_thanh_toan (Ngày thanh toán)
            - ngay_hien_tai (Ngày hiện tại)

            OUTPUT:
            Chỉ trả về HTML thuần. Không markdown. Không giải thích.
            """;

    private static final String EXTRACT_DATA_SYSTEM_PROMPT = """
            Bạn là chuyên gia trích xuất thông tin từ hợp đồng thuê phòng/nhà trọ Việt Nam đã được điền đầy đủ.

            NHIỆM VỤ:
            1. Trích xuất TOÀN BỘ nội dung hợp đồng thành HTML (giữ nguyên 100% văn bản gốc)
            2. Trích xuất các THÔNG TIN CỤ THỂ từ hợp đồng thành dữ liệu có cấu trúc

            YÊU CẦU HTML (rất quan trọng):
            - Dùng <h1> cho tiêu đề chính (VD: "HỢP ĐỒNG THUÊ PHÒNG TRỌ")
            - Dùng <h2> cho các điều khoản lớn (VD: "ĐIỀU 1: THÔNG TIN CÁC BÊN")
            - Dùng <h3> cho tiêu đề phụ
            - Dùng <p> cho MỖI đoạn văn riêng biệt - KHÔNG gộp nhiều đoạn vào 1 thẻ <p>
            - Dùng <ul> hoặc <ol> cho danh sách có dấu gạch đầu dòng hoặc đánh số
            - Dùng <table> với <tr>, <th>, <td> cho bảng biểu
            - Dùng <strong> hoặc <b> cho chữ in đậm
            - Dùng <em> hoặc <i> cho chữ in nghiêng
            - KHÔNG bao tất cả nội dung trong 1 thẻ <p> duy nhất
            - Mỗi dòng/đoạn riêng biệt trong hợp đồng gốc = 1 thẻ <p> riêng

            VÍ DỤ HTML ĐÚNG:
            <h1 style="text-align: center;">HỢP ĐỒNG THUÊ PHÒNG TRỌ</h1>
            <p style="text-align: center;">Số: 01/2024/HDTP</p>
            <h2>ĐIỀU 1: THÔNG TIN CÁC BÊN</h2>
            <p><strong>BÊN CHO THUÊ (Bên A):</strong></p>
            <p>Ông/Bà: Nguyễn Văn A</p>
            <p>Số điện thoại: 0901234567</p>
            <p>CCCD: 123456789012</p>

            OUTPUT FORMAT (JSON):
            {
              "htmlContent": "<!-- Toàn bộ nội dung hợp đồng dạng HTML có cấu trúc đúng -->",
              "suggestedName": "Tên gợi ý cho hợp đồng/mẫu",
              "extractedData": {
                "landlordName": "Tên chủ nhà hoặc null",
                "landlordPhone": "SĐT chủ nhà hoặc null",
                "landlordIdNumber": "CCCD chủ nhà hoặc null",
                "landlordAddress": "Địa chỉ chủ nhà hoặc null",
                "tenantName": "Tên người thuê hoặc null",
                "tenantPhone": "SĐT người thuê hoặc null",
                "tenantIdNumber": "CCCD người thuê hoặc null",
                "tenantDateOfBirth": "Ngày sinh (YYYY-MM-DD) hoặc null",
                "tenantPlaceOfOrigin": "Quê quán hoặc null",
                "tenantPlaceOfResidence": "Địa chỉ thường trú hoặc null",
                "tenantIdIssueDate": "Ngày cấp CCCD (YYYY-MM-DD) hoặc null",
                "tenantIdIssuePlace": "Nơi cấp CCCD hoặc null",
                "tenantGender": "male/female/other hoặc null",
                "propertyName": "Tên nhà trọ hoặc null",
                "propertyAddress": "Địa chỉ nhà trọ hoặc null",
                "roomCode": "Mã/số phòng hoặc null",
                "floor": số tầng hoặc null,
                "areaM2": diện tích m² hoặc null,
                "monthlyRent": tiền thuê/tháng (số) hoặc null,
                "deposit": tiền cọc (số) hoặc null,
                "depositMonths": số tháng cọc hoặc null,
                "paymentDueDay": ngày thanh toán hàng tháng hoặc null,
                "startDate": "Ngày bắt đầu (YYYY-MM-DD) hoặc null",
                "endDate": "Ngày kết thúc (YYYY-MM-DD) hoặc null",
                "durationMonths": số tháng thuê hoặc null,
                "paymentCycle": "MONTHLY/QUARTERLY hoặc null",
                "electricityRate": giá điện/kWh hoặc null,
                "waterRate": giá nước/m³ hoặc null
              }
            }

            LƯU Ý:
            - Với số tiền: chỉ trả về số (không có chữ "đồng", dấu phẩy, dấu chấm)
            - Với ngày tháng: chuyển sang format YYYY-MM-DD
            - Nếu không tìm thấy thông tin: trả về null
            - htmlContent phải giữ nguyên 100% nội dung gốc và có CẤU TRÚC HTML ĐÚNG

            CHỈ TRẢ VỀ JSON. KHÔNG MARKDOWN. KHÔNG GIẢI THÍCH.
            """;

    private static final String SUGGESTED_NAME_PROMPT = """
            Dựa vào nội dung hợp đồng, đề xuất tên ngắn gọn cho mẫu hợp đồng này.
            Ví dụ: "Hợp đồng thuê phòng trọ", "Hợp đồng thuê nhà nguyên căn", "Hợp đồng thuê căn hộ"
            Chỉ trả về tên, không giải thích.
            """;

    public ScanContractResponse scanAndStructure(List<MultipartFile> images) throws IOException {
        log.info("Processing {} images for contract extraction", images.size());

        // Build media list from images
        List<Media> mediaList = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            String contentType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType(contentType))
                    .data(new ByteArrayResource(image.getBytes()))
                    .build();
            mediaList.add(media);
            log.debug("Added image {} of type {} ({} bytes)", i + 1, contentType, image.getSize());
        }

        // Create prompt with images
        SystemMessage systemMessage = new SystemMessage(SYSTEM_PROMPT);

        String userPrompt = "Đây là " + images.size() + " trang hợp đồng thuê phòng. " +
                "Hãy trích xuất và cấu trúc hóa toàn bộ nội dung thành HTML. " +
                "Các trang đã được sắp xếp theo thứ tự từ trang 1.";

        UserMessage userMessage = UserMessage.builder()
                .text(userPrompt)
                .media(mediaList)
                .build();

        // Use model from application.yml (spring.ai.openai.chat.options)
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            log.info("Calling OpenAI Vision API...");
            ChatResponse response = chatModel.call(prompt);
            String html = response.getResult().getOutput().getText();

            // Clean up the response (remove markdown code blocks if present)
            html = cleanHtmlResponse(html);

            log.info("Successfully extracted contract content ({} chars)", html.length());

            // Get suggested name
            String suggestedName = extractSuggestedName(html);

            return new ScanContractResponse(html, suggestedName);
        } catch (Exception e) {
            log.error("Failed to extract contract content: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể trích xuất nội dung hợp đồng: " + e.getMessage(), e);
        }
    }

    private String extractSuggestedName(String html) {
        try {
            // Extract title from h1 tag if present
            int h1Start = html.indexOf("<h1");
            if (h1Start != -1) {
                int contentStart = html.indexOf(">", h1Start) + 1;
                int contentEnd = html.indexOf("</h1>", contentStart);
                if (contentEnd != -1) {
                    String title = html.substring(contentStart, contentEnd)
                            .replaceAll("<[^>]*>", "") // Remove nested tags
                            .trim();
                    if (!title.isEmpty() && title.length() <= 100) {
                        return title;
                    }
                }
            }

            // Fallback: ask AI for name suggestion
            UserMessage userMessage = new UserMessage(
                    SUGGESTED_NAME_PROMPT + "\n\nNội dung:\n" + html.substring(0, Math.min(500, html.length())));
            Prompt prompt = new Prompt(List.of(userMessage));
            ChatResponse response = chatModel.call(prompt);
            return response.getResult().getOutput().getText().trim();
        } catch (Exception e) {
            log.warn("Could not extract suggested name: {}", e.getMessage());
            return "Mẫu hợp đồng từ scan";
        }
    }

    /**
     * Extract both structured data and full HTML content from a filled paper
     * contract.
     * This is used to import existing paper contracts into the system.
     */
    public ExtractContractDataResponse extractContractData(List<MultipartFile> images) throws IOException {
        log.info("Processing {} images for contract data extraction", images.size());

        // Build media list from images
        List<Media> mediaList = new ArrayList<>();
        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            String contentType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType(contentType))
                    .data(new ByteArrayResource(image.getBytes()))
                    .build();
            mediaList.add(media);
            log.debug("Added image {} of type {} ({} bytes)", i + 1, contentType, image.getSize());
        }

        // Create prompt with images
        SystemMessage systemMessage = new SystemMessage(EXTRACT_DATA_SYSTEM_PROMPT);

        String userPrompt = "Đây là " + images.size() + " trang hợp đồng thuê phòng đã được điền đầy đủ thông tin. " +
                "Hãy trích xuất toàn bộ nội dung thành HTML và trích xuất các thông tin cụ thể thành JSON. " +
                "Các trang đã được sắp xếp theo thứ tự từ trang 1.";

        UserMessage userMessage = UserMessage.builder()
                .text(userPrompt)
                .media(mediaList)
                .build();

        Prompt prompt = new Prompt(List.of(systemMessage, userMessage));

        try {
            log.info("Calling OpenAI Vision API for data extraction...");
            ChatResponse response = chatModel.call(prompt);
            String jsonResponse = response.getResult().getOutput().getText();

            // Clean up the response (remove markdown code blocks if present)
            jsonResponse = cleanJsonResponse(jsonResponse);

            log.info("Successfully extracted contract data ({} chars)", jsonResponse.length());

            // Parse JSON response
            ExtractContractDataResponse result = objectMapper.readValue(jsonResponse,
                    ExtractContractDataResponse.class);

            // Clean htmlContent if it contains markdown blocks
            if (result.htmlContent() != null) {
                String cleanedHtml = cleanHtmlResponse(result.htmlContent());
                result = new ExtractContractDataResponse(
                        cleanedHtml,
                        result.suggestedName(),
                        result.extractedData());
            }

            return result;
        } catch (Exception e) {
            log.error("Failed to extract contract data: {}", e.getMessage(), e);
            throw new RuntimeException("Không thể trích xuất dữ liệu hợp đồng: " + e.getMessage(), e);
        }
    }

    private String cleanJsonResponse(String json) {
        // Remove markdown code blocks if present
        if (json.startsWith("```json")) {
            json = json.substring(7);
        } else if (json.startsWith("```")) {
            json = json.substring(3);
        }
        if (json.endsWith("```")) {
            json = json.substring(0, json.length() - 3);
        }
        return json.trim();
    }

    private String cleanHtmlResponse(String html) {
        if (html == null)
            return null;

        // Remove markdown code blocks if present
        String cleaned = html.trim();

        // Remove opening code block markers
        if (cleaned.startsWith("```html")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```HTML")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        // Remove closing code block markers
        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        return cleaned.trim();
    }
}
