package com.pegacorn.rently.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pegacorn.rently.dto.ai.*;
import com.pegacorn.rently.dto.expense.CreateExpenseRequest;
import com.pegacorn.rently.dto.expense.ExpenseDto;
import com.pegacorn.rently.dto.expense.ExpenseSummaryDto;
import com.pegacorn.rently.dto.invoice.InvoiceSummaryDto;
import com.pegacorn.rently.dto.invoice.MeterReadingDto;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.repository.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class AIChatService {

    // Rate limiting
    private static final ConcurrentHashMap<String, Long> lastRequestTime = new ConcurrentHashMap<>();

    private final long rateLimitMs;
    private final OpenAiChatModel chatModel;
    private final ObjectMapper objectMapper;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;
    private final MeterReadingRepository meterReadingRepository;
    private final InvoiceService invoiceService;
    private final ExpenseService expenseService;
    private final RoomService roomService;

    public AIChatService(
            @Value("${ai-chat.cooldown-seconds:300}") int cooldownSeconds,
            OpenAiChatModel chatModel,
            ObjectMapper objectMapper,
            HouseRepository houseRepository,
            RoomRepository roomRepository,
            ContractRepository contractRepository,
            InvoiceRepository invoiceRepository,
            ExpenseRepository expenseRepository,
            UserRepository userRepository,
            MeterReadingRepository meterReadingRepository,
            InvoiceService invoiceService,
            ExpenseService expenseService,
            RoomService roomService) {
        this.rateLimitMs = cooldownSeconds * 1000L;
        this.chatModel = chatModel;
        this.objectMapper = objectMapper;
        this.houseRepository = houseRepository;
        this.roomRepository = roomRepository;
        this.contractRepository = contractRepository;
        this.invoiceRepository = invoiceRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
        this.meterReadingRepository = meterReadingRepository;
        this.invoiceService = invoiceService;
        this.expenseService = expenseService;
        this.roomService = roomService;
    }

    // ==================== PROMPTS ====================

    private static final String INTENT_ANALYSIS_PROMPT = """
            Bạn là AI phân tích intent. Nhiệm vụ: phân tích câu hỏi của user và xác định cần dữ liệu gì.

            Trả về JSON với format:
            {
              "queryType": "STATISTICS|LIST_DATA|SINGLE_ITEM|ACTION_REQUEST|HELP_QUESTION|GENERAL_CHAT",
              "needHouses": true/false,
              "needRooms": true/false,
              "houseNameFilter": "tên nhà cụ thể hoặc null",
              "needContracts": true/false,
              "roomCodeFilter": "mã phòng cụ thể hoặc null",
              "needInvoices": true/false,
              "invoiceMonth": số tháng hoặc null,
              "invoiceYear": số năm hoặc null,
              "needExpenses": true/false,
              "expenseMonth": số tháng hoặc null,
              "expenseYear": số năm hoặc null,
              "needMeterReadings": true/false,
              "meterRoomCode": "mã phòng hoặc null",
              "meterPeriod": "yyyy-MM hoặc null",
              "isSimpleQuestion": true/false,
              "isActionRequest": true/false,
              "actionType": "CREATE_INVOICE|MARK_INVOICE_PAID|CREATE_EXPENSE|DELETE_EXPENSE|RECORD_METER|SHOW_EXPENSE|SHOW_INCOME|NAVIGATE|null",
              "isOffTopic": true/false,
              "intentSummary": "tóm tắt ngắn gọn ý định user"
            }

            Quy tắc:
            - Nếu hỏi về hướng dẫn sử dụng website → isSimpleQuestion=true, không cần data
            - Nếu hỏi thống kê phòng → needHouses=true, needRooms=true
            - Nếu hỏi về hóa đơn/thu nhập → needInvoices=true
            - Nếu hỏi về chi phí → needExpenses=true
            - Nếu yêu cầu tạo/xóa/sửa → isActionRequest=true + actionType phù hợp
            - Nếu câu hỏi không liên quan đến hệ thống Rently, quản lý nhà trọ, phòng, hợp đồng, chi phí hoặc tính năng của website (ví dụ: hỏi về nấu ăn, thời tiết, chính trị, v.v.) → isOffTopic=true. Chào hỏi thông thường KHÔNG phải là off-topic.
            - Tháng/năm hiện tại: %s

            LUÔN trả về JSON hợp lệ, không có text ngoài JSON.
            """;

    private static final String RESPONSE_GENERATION_PROMPT = """
            Bạn là trợ lý AI của hệ thống quản lý nhà trọ Rently.

            ## HƯỚNG DẪN SỬ DỤNG WEBSITE:
            ### Quản lý nhà: Vào menu "Nhà trọ" > "Thêm nhà" (tên, địa chỉ, số tầng)
            ### Quản lý phòng: Trong chi tiết nhà > "Thêm phòng" (mã, tầng, diện tích, giá)
            ### Thêm người thuê: Vào phòng trống > "Thêm người thuê" (có thể quét QR CCCD)
            ### Tạo hợp đồng: Sau thêm người thuê > tạo hợp đồng (ngày bắt đầu/kết thúc, tiền thuê, cọc)
            ### Ghi điện nước: Menu "Điện nước" > chọn tháng > nhập chỉ số
            ### Tạo hóa đơn: Menu "Hóa đơn" > "Tạo hóa đơn" > chọn phòng và kỳ
            ### Quản lý thu chi: Menu "Thu chi" để xem báo cáo

            ## DỮ LIỆU HIỆN TẠI:
            %s

            ## XỬ LÝ HÀNH ĐỘNG:
            CHỈ trả về action khi user yêu cầu RÕ RÀNG và có ĐỦ THÔNG TIN.

            Khi trả về action, message PHẢI NGẮN GỌN (1-2 câu). Ví dụ:
            - "Thêm người thuê Nguyễn Văn A vào phòng P201."
            - "Tạo hóa đơn tháng 01/2025 cho phòng P201."
            - "Đánh dấu đã thanh toán hóa đơn phòng P201."

            KHÔNG viết dài dòng, không liệt kê chi tiết. Chi tiết sẽ hiển thị trong card xác nhận.

            Format JSON:
            {
              "message": "Câu ngắn gọn mô tả hành động",
              "actionType": "ACTION_TYPE",
              "actionData": { ...data... }
            }

            KHÔNG trả về action khi:
            - Đang hỏi thêm thông tin/làm rõ yêu cầu
            - Thông tin chưa đầy đủ để thực hiện
            - User chỉ hỏi thông tin, không yêu cầu thực hiện

            Khi KHÔNG có action:
            { "message": "Nội dung trả lời", "actionType": null, "actionData": null }

            Action types và constraints:

            CREATE_INVOICE:
            - roomCode (bắt buộc): mã phòng
            - periodMonth: kỳ hóa đơn yyyy-MM (mặc định tháng hiện tại)

            MARK_INVOICE_PAID:
            - roomCode (bắt buộc): mã phòng
            - periodMonth: kỳ hóa đơn

            CREATE_EXPENSE:
            - description (bắt buộc): mô tả chi phí
            - amount (bắt buộc): số tiền (số nguyên, VD: 500000)

            DELETE_EXPENSE:
            - expenseId (bắt buộc): ID khoản chi

            RECORD_METER:
            - roomCode (bắt buộc): mã phòng
            - electricityNew hoặc waterNew (ít nhất 1): chỉ số mới

            SHOW_EXPENSE/SHOW_INCOME:
            - month, year: tháng/năm (mặc định hiện tại)

            ADD_TENANT:
            - roomCode (bắt buộc): mã phòng
            - fullName (bắt buộc): họ tên đầy đủ
            - phone (bắt buộc): SĐT 10 số, bắt đầu bằng 0 (VD: 0901234567)
            - idNumber (bắt buộc): CCCD 12 số (VD: 001234567890)
            - isPrimary: người thuê chính (true/false, mặc định false)
            Nếu user nói "fake/giả/test", tự tạo dữ liệu mẫu hợp lệ.

            KHÔNG hỗ trợ: xóa người thuê, tạo hợp đồng, kết thúc hợp đồng, sửa hợp đồng

            ## QUY TẮC FORMAT:
            1. LUÔN trả lời NGẮN GỌN
            2. KHÔNG hiển thị UUID/ID
            3. KHÔNG dùng emoji/icon
            4. LUÔN trả về JSON hợp lệ
            5. Khi không hỗ trợ: "Tôi không thể [X] trực tiếp. Bạn có cần hướng dẫn không?"
            6. KHÔNG dùng "Tổng quan:", chỉ trả lời thẳng vào vấn đề
            7. Đề xuất (nếu có) chỉ 1-2 gợi ý ngắn, VD: "Thêm người thuê?" hoặc "Tạo hóa đơn?"

            Tháng/năm hiện tại: %s
            """;

    // ==================== MAIN CHAT METHOD ====================

    public ChatResponseDto chat(ChatRequest request, String landlordId) {
        log.info("Processing chat request for landlord: {}", landlordId);

        // Check rate limit
        Long lastTime = lastRequestTime.get(landlordId);
        long now = System.currentTimeMillis();
        if (lastTime != null) {
            long elapsed = now - lastTime;
            if (elapsed < rateLimitMs) {
                long remainingSeconds = (rateLimitMs - elapsed) / 1000;
                long minutes = remainingSeconds / 60;
                long seconds = remainingSeconds % 60;
                String timeRemaining = minutes > 0
                    ? String.format("%d phút %d giây", minutes, seconds)
                    : String.format("%d giây", seconds);
                return new ChatResponseDto(
                        String.format("Vui lòng đợi %s trước khi gửi câu hỏi tiếp theo.", timeRemaining),
                        null,
                        null
                );
            }
        }

        // Update last request time
        lastRequestTime.put(landlordId, now);

        String currentDate = LocalDate.now().format(DateTimeFormatter.ofPattern("MM/yyyy"));

        try {
            // STEP 1: Analyze user intent
            DataRequirementDto requirements = analyzeIntent(request, currentDate);
            log.debug("Intent analysis result: {}", requirements);

            // Skip further calls if off-topic
            if (requirements.isOffTopic()) {
                return new ChatResponseDto(
                        "Tôi là trợ lý AI chuyên về hệ thống quản lý nhà trọ Rently. Tôi chỉ có thể hỗ trợ các vấn đề liên quan đến nhà trọ, phòng, hợp đồng, chi phí và các tính năng của website. Bạn có câu hỏi nào về hệ thống không?",
                        null,
                        null
                );
            }

            // STEP 2: Fetch required data
            FetchedDataDto fetchedData = fetchRequiredData(requirements, landlordId);
            log.debug("Fetched data: {} houses, {} rooms, {} invoices, {} expenses",
                    fetchedData.houses().size(),
                    fetchedData.rooms().size(),
                    fetchedData.invoices().size(),
                    fetchedData.expenses().size());

            // STEP 3: Generate response with data
            return generateResponse(request, fetchedData, requirements, landlordId, currentDate);

        } catch (Exception e) {
            log.error("Chat error: {}", e.getMessage(), e);
            return new ChatResponseDto(
                    "Xin lỗi, đã có lỗi xảy ra. Vui lòng thử lại sau.",
                    null,
                    null
            );
        }
    }

    // ==================== STEP 1: INTENT ANALYSIS ====================

    private DataRequirementDto analyzeIntent(ChatRequest request, String currentDate) {
        try {
            String prompt = String.format(INTENT_ANALYSIS_PROMPT, currentDate);

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(prompt));

            // Add recent history for context (last 2 exchanges)
            if (request.history() != null && !request.history().isEmpty()) {
                int start = Math.max(0, request.history().size() - 4);
                for (int i = start; i < request.history().size(); i++) {
                    ChatMessageDto msg = request.history().get(i);
                    if ("user".equals(msg.role())) {
                        messages.add(new UserMessage(msg.content()));
                    } else {
                        messages.add(new AssistantMessage(msg.content()));
                    }
                }
            }

            messages.add(new UserMessage(request.message()));

            ChatResponse response = chatModel.call(new Prompt(messages));
            String content = cleanJsonResponse(response.getResult().getOutput().getText());

            log.debug("Intent analysis raw response: {}", content);

            return objectMapper.readValue(content, DataRequirementDto.class);

        } catch (Exception e) {
            log.warn("Intent analysis failed, using default: {}", e.getMessage());
            // Default: fetch basic data
            return new DataRequirementDto(
                    DataRequirementDto.QueryType.GENERAL_CHAT,
                    true, true, null, false, null,
                    false, null, null, false, null, null,
                    false, null, null, false, false, null,
                    false, request.message()
            );
        }
    }

    // ==================== STEP 2: DATA FETCHING ====================

    private FetchedDataDto fetchRequiredData(DataRequirementDto req, String landlordId) {
        List<FetchedDataDto.HouseData> houses = new ArrayList<>();
        List<FetchedDataDto.RoomData> rooms = new ArrayList<>();
        List<FetchedDataDto.ContractData> contracts = new ArrayList<>();
        List<FetchedDataDto.InvoiceData> invoices = new ArrayList<>();
        List<FetchedDataDto.ExpenseData> expenses = new ArrayList<>();
        List<FetchedDataDto.MeterData> meterReadings = new ArrayList<>();

        int totalRooms = 0, emptyRooms = 0, rentedRooms = 0;
        BigDecimal totalInvoice = BigDecimal.ZERO, paidInvoice = BigDecimal.ZERO, totalExpense = BigDecimal.ZERO;

        try {
            // Fetch houses if needed
            if (req.needHouses() || req.needRooms()) {
                List<House> houseEntities = houseRepository.findByOwnerId(landlordId);

                for (House house : houseEntities) {
                    // Filter by house name if specified
                    if (req.houseNameFilter() != null &&
                            !house.getName().toLowerCase().contains(req.houseNameFilter().toLowerCase())) {
                        continue;
                    }

                    List<Room> roomEntities = roomRepository.findByHouseId(house.getId());
                    int houseEmpty = 0, houseRented = 0;

                    for (Room room : roomEntities) {
                        // Filter by room code if specified
                        if (req.roomCodeFilter() != null &&
                                !room.getCode().equalsIgnoreCase(req.roomCodeFilter())) {
                            continue;
                        }

                        String status = switch (room.getStatus()) {
                            case EMPTY -> { houseEmpty++; emptyRooms++; yield "Trống"; }
                            case RENTED -> { houseRented++; rentedRooms++; yield "Đang thuê"; }
                            case MAINTENANCE -> "Bảo trì";
                            default -> "Không xác định";
                        };
                        totalRooms++;

                        // Get tenant name from contract (ACTIVE or DRAFT)
                        String tenantName = null;
                        List<Contract> roomContracts = contractRepository.findByRoomId(room.getId());
                        for (Contract contract : roomContracts) {
                            if (contract.getStatus() == Contract.ContractStatus.ACTIVE ||
                                contract.getStatus() == Contract.ContractStatus.DRAFT) {
                                tenantName = userRepository.findById(contract.getTenantId())
                                        .map(User::getFullName).orElse(null);
                                if (tenantName != null) break;
                            }
                        }

                        rooms.add(new FetchedDataDto.RoomData(
                                room.getId(), room.getCode(), house.getName(),
                                status, room.getBaseRent(), tenantName
                        ));
                    }

                    houses.add(new FetchedDataDto.HouseData(
                            house.getId(), house.getName(), house.getAddress(),
                            roomEntities.size(), houseEmpty, houseRented
                    ));
                }
            }

            // Fetch contracts if needed (ACTIVE and DRAFT)
            if (req.needContracts()) {
                List<Contract> contractEntities = contractRepository.findByLandlordId(landlordId);
                for (Contract contract : contractEntities) {
                    if (contract.getStatus() != Contract.ContractStatus.ACTIVE &&
                        contract.getStatus() != Contract.ContractStatus.DRAFT) continue;

                    String roomCode = roomRepository.findById(contract.getRoomId())
                            .map(Room::getCode).orElse("N/A");
                    String houseName = roomRepository.findById(contract.getRoomId())
                            .flatMap(room -> houseRepository.findById(room.getHouseId()))
                            .map(House::getName).orElse("N/A");
                    String tenantName = userRepository.findById(contract.getTenantId())
                            .map(User::getFullName).orElse("N/A");

                    contracts.add(new FetchedDataDto.ContractData(
                            contract.getId(), roomCode, houseName, tenantName,
                            contract.getStatus().name(), contract.getMonthlyRent(),
                            contract.getStartDate().toString(),
                            contract.getEndDate() != null ? contract.getEndDate().toString() : null
                    ));
                }
            }

            // Fetch invoices if needed
            if (req.needInvoices()) {
                int month = req.invoiceMonth() != null ? req.invoiceMonth() : LocalDate.now().getMonthValue();
                int year = req.invoiceYear() != null ? req.invoiceYear() : LocalDate.now().getYear();
                String periodMonth = String.format("%d-%02d", year, month);

                List<Invoice> invoiceEntities = invoiceRepository.findByLandlordIdAndPeriodMonth(landlordId, periodMonth);
                for (Invoice invoice : invoiceEntities) {
                    String roomCode = contractRepository.findById(invoice.getContractId())
                            .flatMap(c -> roomRepository.findById(c.getRoomId()))
                            .map(Room::getCode).orElse("N/A");
                    String tenantName = userRepository.findById(invoice.getTenantId())
                            .map(User::getFullName).orElse("N/A");

                    totalInvoice = totalInvoice.add(invoice.getTotalAmount());
                    paidInvoice = paidInvoice.add(invoice.getPaidAmount());

                    invoices.add(new FetchedDataDto.InvoiceData(
                            invoice.getId(), roomCode, tenantName, invoice.getPeriodMonth(),
                            invoice.getTotalAmount(), invoice.getPaidAmount(),
                            invoice.getStatus().name(),
                            invoice.getDueDate() != null ? invoice.getDueDate().toString() : null
                    ));
                }
            }

            // Fetch expenses if needed
            if (req.needExpenses()) {
                int month = req.expenseMonth() != null ? req.expenseMonth() : LocalDate.now().getMonthValue();
                int year = req.expenseYear() != null ? req.expenseYear() : LocalDate.now().getYear();

                List<Expense> expenseEntities = expenseRepository.findByLandlordIdAndMonthYear(landlordId, month, year);
                for (Expense expense : expenseEntities) {
                    String houseName = houseRepository.findById(expense.getHouseId())
                            .map(House::getName).orElse("N/A");
                    ExpenseCategoryType category = ExpenseCategoryType.fromId(expense.getCategoryId());
                    String categoryName = category != null ? category.getNameVi() : "Khác";

                    totalExpense = totalExpense.add(expense.getAmount());

                    expenses.add(new FetchedDataDto.ExpenseData(
                            expense.getId(), expense.getTitle(), categoryName, houseName,
                            expense.getAmount(), expense.getStatus().name(),
                            expense.getExpenseDate().toString()
                    ));
                }
            }

            // Fetch meter readings if needed
            if (req.needMeterReadings() && req.meterRoomCode() != null) {
                Room room = findRoomByCode(req.meterRoomCode(), landlordId);
                if (room != null) {
                    String period = req.meterPeriod() != null ? req.meterPeriod() :
                            LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    meterReadingRepository.findByRoomIdAndPeriodMonth(room.getId(), period)
                            .ifPresent(mr -> meterReadings.add(new FetchedDataDto.MeterData(
                                    req.meterRoomCode(), period,
                                    mr.getElectricityOld(), mr.getElectricityNew(),
                                    mr.getWaterOld(), mr.getWaterNew()
                            )));
                }
            }

        } catch (Exception e) {
            log.error("Error fetching data: {}", e.getMessage(), e);
        }

        return new FetchedDataDto(
                houses, rooms, contracts, invoices, expenses, meterReadings,
                new FetchedDataDto.SummaryData(
                        houses.size(), totalRooms, emptyRooms, rentedRooms,
                        totalInvoice, paidInvoice, totalExpense
                )
        );
    }

    // ==================== STEP 3: RESPONSE GENERATION ====================

    private ChatResponseDto generateResponse(ChatRequest request, FetchedDataDto data,
                                              DataRequirementDto requirements, String landlordId, String currentDate) {
        try {
            // Build data context string
            String dataContext = buildDataContext(data, requirements);

            String prompt = String.format(RESPONSE_GENERATION_PROMPT, dataContext, currentDate);

            List<Message> messages = new ArrayList<>();
            messages.add(new SystemMessage(prompt));

            // Add conversation history
            if (request.history() != null) {
                for (ChatMessageDto msg : request.history()) {
                    if ("user".equals(msg.role())) {
                        messages.add(new UserMessage(msg.content()));
                    } else {
                        messages.add(new AssistantMessage(msg.content()));
                    }
                }
            }

            messages.add(new UserMessage(request.message()));

            ChatResponse response = chatModel.call(new Prompt(messages));
            String content = response.getResult().getOutput().getText();

            log.debug("Response generation raw output: {}", content);

            return parseResponse(content, landlordId);

        } catch (Exception e) {
            log.error("Response generation error: {}", e.getMessage(), e);
            return new ChatResponseDto(
                    "Xin lỗi, đã có lỗi xảy ra khi xử lý yêu cầu.",
                    null, null
            );
        }
    }

    private String buildDataContext(FetchedDataDto data, DataRequirementDto req) {
        StringBuilder ctx = new StringBuilder();

        // Summary
        if (data.summary() != null) {
            var s = data.summary();
            if (s.totalHouses() > 0) {
                ctx.append(String.format("Tổng quan: %d nhà, %d phòng (Trống: %d, Đang thuê: %d)\n",
                        s.totalHouses(), s.totalRooms(), s.emptyRooms(), s.rentedRooms()));
            }
            if (s.totalInvoiceAmount().compareTo(BigDecimal.ZERO) > 0) {
                ctx.append(String.format("Hóa đơn: Tổng %s, Đã thu %s\n",
                        formatCurrency(s.totalInvoiceAmount()), formatCurrency(s.paidInvoiceAmount())));
            }
            if (s.totalExpenseAmount().compareTo(BigDecimal.ZERO) > 0) {
                ctx.append(String.format("Chi phí: Tổng %s\n", formatCurrency(s.totalExpenseAmount())));
            }
        }

        // Houses
        if (!data.houses().isEmpty()) {
            ctx.append("\n### Danh sách nhà:\n");
            for (var h : data.houses()) {
                ctx.append(String.format("- %s: %d phòng (Trống: %d, Đang thuê: %d)\n",
                        h.name(), h.totalRooms(), h.emptyRooms(), h.rentedRooms()));
            }
        }

        // Rooms
        if (!data.rooms().isEmpty()) {
            ctx.append("\n### Danh sách phòng:\n");
            for (var r : data.rooms()) {
                ctx.append(String.format("- %s (%s): %s, Giá: %s",
                        r.code(), r.houseName(), r.status(), formatCurrency(r.baseRent())));
                if (r.tenantName() != null) {
                    ctx.append(String.format(", Người thuê: %s", r.tenantName()));
                }
                ctx.append("\n");
            }
        }

        // Contracts
        if (!data.contracts().isEmpty()) {
            ctx.append("\n### Hợp đồng đang hoạt động:\n");
            for (var c : data.contracts()) {
                ctx.append(String.format("- Phòng %s (%s): %s, Tiền thuê: %s\n",
                        c.roomCode(), c.houseName(), c.tenantName(), formatCurrency(c.monthlyRent())));
            }
        }

        // Invoices
        if (!data.invoices().isEmpty()) {
            ctx.append("\n### Hóa đơn:\n");
            for (var i : data.invoices()) {
                ctx.append(String.format("- Phòng %s (%s): %s/%s, Trạng thái: %s\n",
                        i.roomCode(), i.tenantName(),
                        formatCurrency(i.paidAmount()), formatCurrency(i.totalAmount()),
                        translateStatus(i.status())));
            }
        }

        // Expenses
        if (!data.expenses().isEmpty()) {
            ctx.append("\n### Chi phí:\n");
            for (var e : data.expenses()) {
                ctx.append(String.format("- %s (%s): %s, Ngày: %s\n",
                        e.title(), e.category(), formatCurrency(e.amount()), e.expenseDate()));
            }
        }

        // Meter readings
        if (!data.meterReadings().isEmpty()) {
            ctx.append("\n### Chỉ số điện nước:\n");
            for (var m : data.meterReadings()) {
                ctx.append(String.format("- Phòng %s kỳ %s: Điện %s→%s, Nước %s→%s\n",
                        m.roomCode(), m.periodMonth(),
                        m.electricityOld(), m.electricityNew(),
                        m.waterOld(), m.waterNew()));
            }
        }

        if (ctx.length() == 0) {
            ctx.append("Không có dữ liệu phù hợp với yêu cầu.");
        }

        return ctx.toString();
    }

    // ==================== HELPERS ====================

    private String cleanJsonResponse(String content) {
        content = content.trim();
        if (content.startsWith("```json")) content = content.substring(7);
        if (content.startsWith("```")) content = content.substring(3);
        if (content.endsWith("```")) content = content.substring(0, content.length() - 3);
        return content.trim();
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0đ";
        return String.format("%,dđ", amount.longValue());
    }

    private String translateStatus(String status) {
        return switch (status) {
            case "PENDING" -> "Chờ thanh toán";
            case "PAID" -> "Đã thanh toán";
            case "OVERDUE" -> "Quá hạn";
            case "CANCELLED" -> "Đã hủy";
            default -> status;
        };
    }

    private Room findRoomByCode(String roomCode, String landlordId) {
        List<House> houses = houseRepository.findByOwnerId(landlordId);
        for (House house : houses) {
            List<Room> rooms = roomRepository.findByHouseId(house.getId());
            for (Room room : rooms) {
                if (room.getCode().equalsIgnoreCase(roomCode)) {
                    return room;
                }
            }
        }
        return null;
    }

    private ChatResponseDto parseResponse(String content, String landlordId) {
        content = cleanJsonResponse(content);

        try {
            Map<String, Object> parsed = objectMapper.readValue(content, Map.class);
            String message = (String) parsed.get("message");
            String actionType = (String) parsed.get("actionType");
            Object actionData = parsed.get("actionData");

            // Validate action has required data
            if (actionType != null && actionData != null) {
                Map<String, Object> dataMap = (Map<String, Object>) actionData;
                if (!isValidAction(actionType, dataMap)) {
                    log.warn("Invalid action data for {}: {}", actionType, dataMap);
                    return new ChatResponseDto(message, null, null);
                }
                actionData = enrichActionData(actionType, dataMap, landlordId);
            }

            return new ChatResponseDto(message, actionType, actionData);

        } catch (JsonProcessingException e) {
            log.debug("Response is not JSON, treating as plain text");
            return new ChatResponseDto(content, null, null);
        }
    }

    private boolean isValidAction(String actionType, Map<String, Object> data) {
        if (data == null || data.isEmpty()) return false;

        return switch (actionType) {
            case "CREATE_INVOICE", "MARK_INVOICE_PAID" ->
                data.get("roomCode") != null;
            case "CREATE_EXPENSE" ->
                data.get("description") != null && data.get("amount") != null;
            case "DELETE_EXPENSE" ->
                data.get("expenseId") != null;
            case "RECORD_METER" ->
                data.get("roomCode") != null &&
                (data.get("electricityNew") != null || data.get("waterNew") != null);
            case "SHOW_EXPENSE", "SHOW_INCOME" ->
                true; // month/year can default to current
            case "ADD_TENANT" ->
                data.get("roomCode") != null &&
                data.get("phone") != null &&
                data.get("fullName") != null &&
                data.get("idNumber") != null;
            default -> false;
        };
    }

    // ==================== ACTION EXECUTION (unchanged) ====================

    public ExecuteActionResponse executeAction(ExecuteActionRequest request, String landlordId) {
        log.info("Executing action: {} for landlord: {}", request.actionType(), landlordId);

        try {
            return switch (request.actionType()) {
                case "CREATE_INVOICE" -> executeCreateInvoice(request.actionData(), landlordId);
                case "MARK_INVOICE_PAID" -> executeMarkInvoicePaid(request.actionData(), landlordId);
                case "CREATE_EXPENSE" -> executeCreateExpense(request.actionData(), landlordId);
                case "DELETE_EXPENSE" -> executeDeleteExpense(request.actionData(), landlordId);
                case "RECORD_METER" -> executeRecordMeter(request.actionData(), landlordId);
                case "ADD_TENANT" -> executeAddTenant(request.actionData(), landlordId);
                default -> ExecuteActionResponse.error("Hành động không được hỗ trợ: " + request.actionType());
            };
        } catch (Exception e) {
            log.error("Error executing action {}: {}", request.actionType(), e.getMessage(), e);
            return ExecuteActionResponse.error("Lỗi: " + e.getMessage());
        }
    }

    private ExecuteActionResponse executeCreateInvoice(Map<String, Object> actionData, String landlordId) {
        String contractId = (String) actionData.get("contractId");
        String periodMonth = (String) actionData.get("periodMonth");

        if (contractId == null) {
            return ExecuteActionResponse.error("Phòng này chưa có hợp đồng đang hoạt động");
        }

        String redirectUrl = String.format("/invoices/create?contractId=%s&periodMonth=%s", contractId, periodMonth);
        return ExecuteActionResponse.success(
                "Đang mở trang tạo hóa đơn...",
                Map.of("redirect", redirectUrl, "contractId", contractId, "periodMonth", periodMonth)
        );
    }

    private ExecuteActionResponse executeMarkInvoicePaid(Map<String, Object> actionData, String landlordId) {
        String roomCode = (String) actionData.get("roomCode");
        String periodMonth = (String) actionData.get("periodMonth");
        String contractId = (String) actionData.get("contractId");

        if (contractId == null) {
            return ExecuteActionResponse.error("Không tìm thấy hợp đồng cho phòng " + roomCode);
        }

        var invoices = invoiceRepository.findByContractIdAndPeriodMonth(contractId, periodMonth);
        if (invoices.isEmpty()) {
            return ExecuteActionResponse.error("Không tìm thấy hóa đơn cho phòng " + roomCode + " kỳ " + periodMonth);
        }

        Invoice invoice = invoices.get(0);
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            return ExecuteActionResponse.error("Hóa đơn này đã được thanh toán");
        }

        var remainingAmount = invoice.getTotalAmount().subtract(invoice.getPaidAmount());
        invoiceService.recordManualPayment(invoice.getId(), remainingAmount, "CASH", "Thanh toán qua AI Assistant", landlordId);

        return ExecuteActionResponse.success(
                "Đã ghi nhận thanh toán " + formatCurrency(remainingAmount) + " cho phòng " + roomCode,
                Map.of("invoiceId", invoice.getId(), "amount", remainingAmount)
        );
    }

    private ExecuteActionResponse executeCreateExpense(Map<String, Object> actionData, String landlordId) {
        String description = (String) actionData.get("description");
        Number amountNum = (Number) actionData.get("amount");
        String category = (String) actionData.get("category");
        String houseId = (String) actionData.get("houseId");

        if (description == null || amountNum == null) {
            return ExecuteActionResponse.error("Thiếu thông tin chi phí (mô tả hoặc số tiền)");
        }

        String categoryId = findCategoryId(landlordId, category);
        if (categoryId == null) {
            return ExecuteActionResponse.error("Không tìm thấy danh mục chi phí");
        }

        if (houseId == null) {
            List<House> houses = houseRepository.findByOwnerId(landlordId);
            if (houses.isEmpty()) {
                return ExecuteActionResponse.error("Bạn chưa có nhà trọ nào");
            }
            houseId = houses.get(0).getId();
        }

        BigDecimal amount = BigDecimal.valueOf(amountNum.doubleValue());
        String today = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

        CreateExpenseRequest createRequest = new CreateExpenseRequest(houseId, categoryId, description, null, amount, today, null, null);
        ExpenseDto expense = expenseService.create(createRequest, landlordId, null);

        return ExecuteActionResponse.success(
                "Đã tạo khoản chi \"" + description + "\" với số tiền " + formatCurrency(amount),
                expense
        );
    }

    private ExecuteActionResponse executeDeleteExpense(Map<String, Object> actionData, String landlordId) {
        String expenseId = (String) actionData.get("expenseId");
        if (expenseId == null) {
            return ExecuteActionResponse.error("Thiếu ID khoản chi cần xóa");
        }
        expenseService.delete(expenseId, landlordId);
        return ExecuteActionResponse.success("Đã xóa khoản chi", Map.of("expenseId", expenseId));
    }

    private ExecuteActionResponse executeRecordMeter(Map<String, Object> actionData, String landlordId) {
        String roomId = (String) actionData.get("roomId");
        String periodMonth = (String) actionData.get("periodMonth");
        Number electricityNew = (Number) actionData.get("electricityNew");
        Number waterNew = (Number) actionData.get("waterNew");

        if (roomId == null) {
            return ExecuteActionResponse.error("Không tìm thấy phòng");
        }

        MeterReadingDto previousReading = invoiceService.getLatestMeterReading(roomId, landlordId);

        BigDecimal elecOld = previousReading != null && previousReading.electricityNew() != null
                ? previousReading.electricityNew() : BigDecimal.ZERO;
        BigDecimal waterOld = previousReading != null && previousReading.waterNew() != null
                ? previousReading.waterNew() : BigDecimal.ZERO;

        BigDecimal elecNew = electricityNew != null ? BigDecimal.valueOf(electricityNew.doubleValue()) : elecOld;
        BigDecimal newWater = waterNew != null ? BigDecimal.valueOf(waterNew.doubleValue()) : waterOld;

        MeterReadingDto meterDto = new MeterReadingDto(roomId, periodMonth, elecOld, elecNew, null, waterOld, newWater, null);
        invoiceService.saveMeterReading(meterDto, landlordId);

        String roomCode = (String) actionData.get("roomCode");
        StringBuilder message = new StringBuilder("Đã ghi chỉ số cho phòng " + roomCode + " kỳ " + periodMonth + ": ");
        if (electricityNew != null) message.append("Điện: ").append(elecNew);
        if (waterNew != null) {
            if (electricityNew != null) message.append(", ");
            message.append("Nước: ").append(newWater);
        }

        return ExecuteActionResponse.success(message.toString(), meterDto);
    }

    private ExecuteActionResponse executeAddTenant(Map<String, Object> actionData, String landlordId) {
        String roomId = (String) actionData.get("roomId");
        String roomCode = (String) actionData.get("roomCode");
        String phone = (String) actionData.get("phone");
        String fullName = (String) actionData.get("fullName");
        String idNumber = (String) actionData.get("idNumber");
        Boolean isPrimary = actionData.get("isPrimary") != null ? (Boolean) actionData.get("isPrimary") : false;

        if (roomId == null) {
            return ExecuteActionResponse.error("Không tìm thấy phòng " + roomCode);
        }

        if (phone == null || fullName == null) {
            return ExecuteActionResponse.error("Thiếu thông tin người thuê (tên hoặc số điện thoại)");
        }

        var request = new com.pegacorn.rently.dto.room.AddTenantRequest(
                phone, fullName, idNumber, null, null, null, null, null, null, isPrimary
        );

        roomService.addTenant(roomId, request, landlordId);

        return ExecuteActionResponse.success(
                "Đã thêm người thuê \"" + fullName + "\" vào phòng " + roomCode,
                Map.of("roomId", roomId, "roomCode", roomCode, "fullName", fullName, "phone", phone)
        );
    }

    private String findCategoryId(String landlordId, String categoryType) {
        if (categoryType != null) {
            return switch (categoryType.toUpperCase()) {
                case "ELECTRIC", "ELECTRICITY", "ĐIỆN" -> ExpenseCategoryType.ELECTRIC.getId();
                case "WATER", "NƯỚC" -> ExpenseCategoryType.WATER.getId();
                case "INTERNET", "WIFI" -> ExpenseCategoryType.INTERNET.getId();
                case "TRASH", "RÁC", "GARBAGE" -> ExpenseCategoryType.TRASH.getId();
                case "SECURITY", "BẢO VỆ" -> ExpenseCategoryType.SECURITY.getId();
                case "PARKING", "GỬI XE", "XE" -> ExpenseCategoryType.PARKING.getId();
                case "ELEVATOR", "THANG MÁY" -> ExpenseCategoryType.ELEVATOR.getId();
                case "LAUNDRY", "MÁY GIẶT", "GIẶT" -> ExpenseCategoryType.LAUNDRY.getId();
                default -> ExpenseCategoryType.OTHER.getId();
            };
        }
        return ExpenseCategoryType.OTHER.getId();
    }

    private Object enrichActionData(String actionType, Map<String, Object> actionData, String landlordId) {
        try {
            switch (actionType) {
                case "CREATE_INVOICE", "MARK_INVOICE_PAID", "RECORD_METER" -> {
                    String roomCode = (String) actionData.get("roomCode");
                    String periodMonth = (String) actionData.get("periodMonth");

                    if (periodMonth == null) {
                        periodMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"));
                    }

                    Room targetRoom = findRoomByCode(roomCode, landlordId);
                    if (targetRoom != null) {
                        House targetHouse = roomRepository.findById(targetRoom.getId())
                                .flatMap(r -> houseRepository.findById(r.getHouseId()))
                                .orElse(null);

                        Contract contract = contractRepository
                                .findByRoomIdAndStatus(targetRoom.getId(), Contract.ContractStatus.ACTIVE)
                                .orElse(null);

                        Map<String, Object> enriched = new HashMap<>(actionData);
                        enriched.put("roomId", targetRoom.getId());
                        enriched.put("roomCode", targetRoom.getCode());
                        enriched.put("houseName", targetHouse != null ? targetHouse.getName() : null);
                        enriched.put("periodMonth", periodMonth);
                        if (contract != null) {
                            enriched.put("contractId", contract.getId());
                            enriched.put("monthlyRent", contract.getMonthlyRent());
                            enriched.put("tenantId", contract.getTenantId());
                        }
                        return enriched;
                    }
                }

                case "CREATE_EXPENSE" -> {
                    Map<String, Object> enriched = new HashMap<>(actionData);
                    String houseId = (String) actionData.get("houseId");
                    if (houseId == null) {
                        List<House> houses = houseRepository.findByOwnerId(landlordId);
                        if (!houses.isEmpty()) {
                            enriched.put("houseId", houses.get(0).getId());
                            enriched.put("houseName", houses.get(0).getName());
                        }
                    } else {
                        houseRepository.findById(houseId).ifPresent(house ->
                                enriched.put("houseName", house.getName()));
                    }
                    if (actionData.get("category") == null) {
                        enriched.put("category", "OTHER");
                    }
                    return enriched;
                }

                case "DELETE_EXPENSE" -> {
                    return actionData;
                }

                case "SHOW_EXPENSE" -> {
                    Integer month = actionData.get("month") != null
                            ? ((Number) actionData.get("month")).intValue()
                            : LocalDate.now().getMonthValue();
                    Integer year = actionData.get("year") != null
                            ? ((Number) actionData.get("year")).intValue()
                            : LocalDate.now().getYear();

                    ExpenseSummaryDto summary = expenseService.getSummary(landlordId, null, month, year);

                    Map<String, Object> enriched = new HashMap<>();
                    enriched.put("month", month);
                    enriched.put("year", year);
                    enriched.put("summary", summary);
                    return enriched;
                }

                case "SHOW_INCOME" -> {
                    Integer month = actionData.get("month") != null
                            ? ((Number) actionData.get("month")).intValue()
                            : LocalDate.now().getMonthValue();
                    Integer year = actionData.get("year") != null
                            ? ((Number) actionData.get("year")).intValue()
                            : LocalDate.now().getYear();

                    InvoiceSummaryDto summary = invoiceService.getSummary(landlordId, null, month, year);

                    Map<String, Object> enriched = new HashMap<>();
                    enriched.put("month", month);
                    enriched.put("year", year);
                    enriched.put("summary", summary);
                    return enriched;
                }

                case "ADD_TENANT" -> {
                    String roomCode = (String) actionData.get("roomCode");
                    Room targetRoom = findRoomByCode(roomCode, landlordId);
                    if (targetRoom != null) {
                        House targetHouse = roomRepository.findById(targetRoom.getId())
                                .flatMap(r -> houseRepository.findById(r.getHouseId()))
                                .orElse(null);

                        Map<String, Object> enriched = new HashMap<>(actionData);
                        enriched.put("roomId", targetRoom.getId());
                        enriched.put("roomCode", targetRoom.getCode());
                        enriched.put("houseName", targetHouse != null ? targetHouse.getName() : null);
                        return enriched;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("Error enriching action data: {}", e.getMessage());
        }

        return actionData;
    }
}
