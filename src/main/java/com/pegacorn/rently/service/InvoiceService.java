package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.invoice.CreateInvoiceRequest;
import com.pegacorn.rently.dto.invoice.CreateAdjustmentDto;
import com.pegacorn.rently.dto.invoice.InvoiceDto;
import com.pegacorn.rently.dto.invoice.InvoiceSummaryDto;
import com.pegacorn.rently.dto.invoice.MeterReadingDto;
import com.pegacorn.rently.dto.invoice.UtilityReconciliationDto;
import com.pegacorn.rently.dto.payment.VietQRResponse;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.entity.Notification;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pegacorn.rently.dto.push.PushNotificationRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class InvoiceService {

        private final InvoiceRepository invoiceRepository;
        private final InvoiceItemRepository invoiceItemRepository;
        private final ContractRepository contractRepository;
        private final RoomRepository roomRepository;
        private final HouseRepository houseRepository;
        private final UserRepository userRepository;
        private final MeterReadingRepository meterReadingRepository;
        private final PaymentRepository paymentRepository;
        private final ExpenseRepository expenseRepository;
        private final UtilityShortfallRepository utilityShortfallRepository;
        private final RoomTenantRepository roomTenantRepository;
        private final ActivityLogService activityLogService;
        private final PushNotificationService pushNotificationService;
        private final NotificationService notificationService;
        private final SmsService smsService;

        @Value("${app.base-url:https://rently.vn}")
        private String appBaseUrl;

        // Default unit prices (can be moved to House settings later)
        private static final BigDecimal DEFAULT_ELECTRICITY_PRICE = new BigDecimal("3500");
        private static final BigDecimal DEFAULT_WATER_PRICE = new BigDecimal("15000");

        public List<InvoiceDto> getAllByLandlord(String landlordId, String houseId, String status, String month,
                        String year) {
                // Start with base query by landlord or house
                List<Invoice> invoices;
                if (houseId != null) {
                        invoices = invoiceRepository.findByHouseId(houseId);
                } else {
                        invoices = invoiceRepository.findByLandlordId(landlordId);
                }

                // Apply additional filters in-memory for combined filtering
                var stream = invoices.stream();

                if (status != null) {
                        Invoice.InvoiceStatus statusEnum = Invoice.InvoiceStatus.valueOf(status);
                        stream = stream.filter(inv -> inv.getStatus() == statusEnum);
                }

                if (year != null && month != null) {
                        String period = String.format("%s-%02d", year, Integer.parseInt(month));
                        stream = stream.filter(inv -> inv.getPeriodMonth().equals(period));
                } else if (month != null) {
                        // Check if month is in YYYY-MM format
                        if (month.contains("-")) {
                                // Month is in YYYY-MM format, filter by exact match
                                stream = stream.filter(inv -> inv.getPeriodMonth().equals(month));
                        } else {
                                // Month is numeric (1-12), filter by suffix
                                String suffix = String.format("-%02d", Integer.parseInt(month));
                                stream = stream.filter(inv -> inv.getPeriodMonth().endsWith(suffix));
                        }
                } else if (year != null) {
                        stream = stream.filter(inv -> inv.getPeriodMonth().startsWith(year));
                }

                return stream
                                .map(this::enrichInvoice)
                                .map(inv -> InvoiceDto.fromEntity(inv, hasPendingPayment(inv.getId())))
                                .toList();
        }

        private boolean hasPendingPayment(String invoiceId) {
                return paymentRepository.existsByInvoiceIdAndStatus(invoiceId, Payment.PaymentStatus.PENDING);
        }

        public InvoiceSummaryDto getSummary(String landlordId, String houseId, Integer month, Integer year) {
                List<Invoice> invoices;
                if (houseId != null) {
                        invoices = invoiceRepository.findByHouseId(houseId);
                } else {
                        invoices = invoiceRepository.findByLandlordId(landlordId);
                }

                // Filter by month/year if provided
                if (month != null && year != null) {
                        String periodPrefix = String.format("%d-%02d", year, month);
                        invoices = invoices.stream()
                                        .filter(inv -> inv.getPeriodMonth().startsWith(periodPrefix))
                                        .toList();
                } else if (year != null) {
                        invoices = invoices.stream()
                                        .filter(inv -> inv.getPeriodMonth().startsWith(String.valueOf(year)))
                                        .toList();
                }

                // Exclude DRAFT and CANCELLED invoices from summary
                invoices = invoices.stream()
                                .filter(inv -> inv.getStatus() != Invoice.InvoiceStatus.DRAFT &&
                                                inv.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                                .toList();

                BigDecimal totalAmount = invoices.stream()
                                .map(Invoice::getTotalAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal paidAmount = invoices.stream()
                                .map(inv -> inv.getPaidAmount() != null ? inv.getPaidAmount() : BigDecimal.ZERO)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal pendingAmount = totalAmount.subtract(paidAmount);

                int paidCount = (int) invoices.stream()
                                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PAID)
                                .count();

                int unpaidCount = invoices.size() - paidCount;

                // Get deposit data from contracts
                List<Contract> contracts;
                if (houseId != null) {
                        contracts = contractRepository.findByHouseId(houseId);
                } else {
                        contracts = contractRepository.findByLandlordId(landlordId);
                }

                // Only count active contracts (not ended or draft)
                contracts = contracts.stream()
                                .filter(c -> c.getStatus() == Contract.ContractStatus.ACTIVE)
                                .toList();

                BigDecimal depositCollected = contracts.stream()
                                .filter(Contract::isDepositPaid)
                                .map(Contract::getDepositAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal depositPending = contracts.stream()
                                .filter(c -> !c.isDepositPaid())
                                .map(Contract::getDepositAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                int depositPaidCount = (int) contracts.stream().filter(Contract::isDepositPaid).count();
                int depositPendingCount = (int) contracts.stream().filter(c -> !c.isDepositPaid()).count();

                return new InvoiceSummaryDto(
                                totalAmount,
                                paidAmount,
                                pendingAmount,
                                invoices.size(),
                                paidCount,
                                unpaidCount,
                                depositCollected,
                                depositPending,
                                depositPaidCount,
                                depositPendingCount);
        }

        public InvoiceDto getById(String id, String landlordId) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                Contract contract = contractRepository.findById(invoice.getContractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                if (!contract.getLandlordId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                return InvoiceDto.fromEntity(enrichInvoice(invoice));
        }

        @Transactional
        public InvoiceDto generate(CreateInvoiceRequest request, String landlordId) {
                Contract contract = contractRepository.findById(request.contractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                if (!contract.getLandlordId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
                        throw ApiException.badRequest(MessageConstant.CONTRACT_NOT_ACTIVE);
                }

                // Check for existing invoice (excluding cancelled ones)
                // Only block if trying to create NORMAL invoice when one already exists
                // CUSTOM_EXPENSE and ADJUSTMENT can be created alongside NORMAL invoices
                Invoice.InvoiceType requestType = request.invoiceType() != null
                        ? Invoice.InvoiceType.valueOf(request.invoiceType())
                        : Invoice.InvoiceType.NORMAL;

                if (requestType == Invoice.InvoiceType.NORMAL) {
                        // Block duplicate NORMAL invoices
                        if (invoiceRepository.existsByContractIdAndPeriodMonthAndInvoiceTypeAndStatusNot(
                                        request.contractId(), request.periodMonth(),
                                        Invoice.InvoiceType.NORMAL, Invoice.InvoiceStatus.CANCELLED)) {
                                throw ApiException.conflict(MessageConstant.INVOICE_ALREADY_EXISTS);
                        }
                }
                // CUSTOM_EXPENSE and ADJUSTMENT invoices can always be created

                BigDecimal totalAmount = request.items().stream()
                                .map(CreateInvoiceRequest.ItemRequest::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                Invoice invoice = Invoice.builder()
                                .id(UUID.randomUUID().toString())
                                .contractId(request.contractId())
                                .tenantId(contract.getTenantId())
                                .periodMonth(request.periodMonth())
                                .dueDate(LocalDate.parse(request.dueDate()))
                                .totalAmount(totalAmount)
                                .paidAmount(BigDecimal.ZERO)
                                .lateFeePercent(request.lateFeePercent() != null ? request.lateFeePercent()
                                                : BigDecimal.ZERO)
                                .status(Invoice.InvoiceStatus.DRAFT)
                                .invoiceType(request.invoiceType() != null
                                                ? Invoice.InvoiceType.valueOf(request.invoiceType())
                                                : Invoice.InvoiceType.NORMAL)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                invoiceRepository.save(invoice);

                for (CreateInvoiceRequest.ItemRequest itemRequest : request.items()) {
                        InvoiceItem item = InvoiceItem.builder()
                                        .id(UUID.randomUUID().toString())
                                        .invoiceId(invoice.getId())
                                        .type(itemRequest.type())
                                        .description(itemRequest.description())
                                        .quantity(itemRequest.quantity())
                                        .unitPrice(itemRequest.unitPrice())
                                        .amount(itemRequest.amount())
                                        .createdAt(LocalDateTime.now())
                                        .build();
                        invoiceItemRepository.save(item);
                }

                // Log activity
                Invoice enriched = enrichInvoice(invoice);
                String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "N/A";
                String tenantName = enriched.getTenant() != null ? enriched.getTenant().getFullName() : "N/A";
                activityLogService.logInvoiceCreated(landlordId, invoice.getId(), roomCode, tenantName,
                                totalAmount.toString());

                return InvoiceDto.fromEntity(enriched);
        }

        @Transactional
        public InvoiceDto send(String id, String landlordId) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                Contract contract = contractRepository.findById(invoice.getContractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                if (!contract.getLandlordId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT) {
                        throw ApiException.badRequest(MessageConstant.ONLY_DRAFT_INVOICE_CAN_SEND);
                }

                invoice.setStatus(Invoice.InvoiceStatus.SENT);
                invoice.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(invoice);

                // Log activity
                Invoice enriched = enrichInvoice(invoice);
                String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "N/A";
                String tenantName = enriched.getTenant() != null ? enriched.getTenant().getFullName() : "N/A";
                activityLogService.logInvoiceSent(landlordId, invoice.getId(), roomCode, tenantName);

                // Send notification to tenant
                sendInvoiceNotification(invoice, enriched);

                return InvoiceDto.fromEntity(enriched);
        }

        /**
         * Send SMS notification for an invoice (separate from send action)
         */
        public void sendSms(String id, String landlordId) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                Contract contract = contractRepository.findById(invoice.getContractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                if (!contract.getLandlordId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                Invoice enriched = enrichInvoice(invoice);
                NumberFormat currencyFormatter = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
                String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "";
                String houseName = enriched.getRoom() != null ? enriched.getRoom().getHouseName() : "";
                String roomId = contract.getRoomId();
                String amount = currencyFormatter.format(invoice.getTotalAmount()) + " VNĐ";

                // Get house address
                Room room = roomRepository.findById(roomId).orElse(null);
                House house = room != null ? houseRepository.findById(room.getHouseId()).orElse(null) : null;
                String houseAddress = house != null ? house.getAddress() : "";

                RoomTenant primaryTenant = roomTenantRepository.findPrimaryByRoomId(roomId).orElse(null);
                if (primaryTenant == null) {
                        throw ApiException.badRequest(MessageConstant.NO_PRIMARY_TENANT);
                }

                User tenant = userRepository.findById(primaryTenant.getUserId()).orElse(null);
                if (tenant == null || tenant.getPhone() == null) {
                        throw ApiException.badRequest(MessageConstant.PRIMARY_TENANT_NO_PHONE);
                }

                String invoiceUrl = appBaseUrl + "/invoice/" + invoice.getId();
                String dueDate = invoice.getDueDate().toString();

                boolean sent = smsService.sendInvoiceNotification(
                                tenant.getPhone(),
                                roomCode,
                                houseName,
                                houseAddress,
                                invoice.getPeriodMonth(),
                                amount,
                                dueDate,
                                invoiceUrl);

                if (!sent) {
                        throw ApiException.badRequest(MessageConstant.FAILED_TO_SEND_SMS);
                }
        }

        @Transactional
        public InvoiceDto cancel(String id, String landlordId) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                Contract contract = contractRepository.findById(invoice.getContractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                if (!contract.getLandlordId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
                        throw ApiException.badRequest(MessageConstant.CANNOT_CANCEL_PAID_INVOICE);
                }

                invoice.setStatus(Invoice.InvoiceStatus.CANCELLED);
                invoice.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(invoice);

                // Log activity
                Invoice enriched = enrichInvoice(invoice);
                String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "N/A";
                activityLogService.logInvoiceCancelled(landlordId, invoice.getId(), roomCode);

                return InvoiceDto.fromEntity(enriched);
        }

        @Transactional
        public void delete(String id, String landlordId) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                Contract contract = contractRepository.findById(invoice.getContractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                if (!contract.getLandlordId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                if (invoice.getStatus() != Invoice.InvoiceStatus.DRAFT
                                && invoice.getStatus() != Invoice.InvoiceStatus.CANCELLED) {
                        throw ApiException.badRequest("Chỉ có thể xóa hóa đơn ở trạng thái Nháp hoặc Đã hủy");
                }

                invoiceItemRepository.deleteByInvoiceId(id);
                invoiceRepository.delete(invoice);
        }

        @Transactional
        public void saveMeterReading(MeterReadingDto dto, String landlordId) {
                Room room = roomRepository.findById(dto.roomId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

                House house = houseRepository.findById(room.getHouseId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                MeterReading reading = meterReadingRepository
                                .findByRoomIdAndPeriodMonth(dto.roomId(), dto.periodMonth())
                                .orElse(MeterReading.builder()
                                                .id(UUID.randomUUID().toString())
                                                .roomId(dto.roomId())
                                                .periodMonth(dto.periodMonth())
                                                .createdAt(LocalDateTime.now())
                                                .build());

                reading.setElectricityOld(dto.electricityOld());
                reading.setElectricityNew(dto.electricityNew());
                reading.setElectricityUnitPrice(
                                dto.electricityUnitPrice() != null ? dto.electricityUnitPrice()
                                                : DEFAULT_ELECTRICITY_PRICE);
                reading.setWaterOld(dto.waterOld());
                reading.setWaterNew(dto.waterNew());
                reading.setWaterUnitPrice(dto.waterUnitPrice() != null ? dto.waterUnitPrice() : DEFAULT_WATER_PRICE);
                reading.setUpdatedAt(LocalDateTime.now());

                meterReadingRepository.save(reading);

                // Log activity
                activityLogService.logMeterReadingSaved(landlordId, room.getId(), room.getCode(), dto.periodMonth());
        }

        public List<MeterReadingDto> getMeterReadings(String roomId, String landlordId) {
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

                House house = houseRepository.findById(room.getHouseId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                return meterReadingRepository.findByRoomIdOrderByPeriodMonthDesc(roomId).stream()
                                .map(MeterReadingDto::fromEntity)
                                .toList();
        }

        public MeterReadingDto getLatestMeterReading(String roomId, String landlordId) {
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

                House house = houseRepository.findById(room.getHouseId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                return meterReadingRepository.findByRoomIdOrderByPeriodMonthDesc(roomId).stream()
                                .findFirst()
                                .map(MeterReadingDto::fromEntity)
                                .orElse(null);
        }

        public MeterReadingDto getMeterReadingByPeriod(String roomId, String periodMonth, String landlordId) {
                Room room = roomRepository.findById(roomId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

                House house = houseRepository.findById(room.getHouseId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                return meterReadingRepository.findByRoomIdAndPeriodMonth(roomId, periodMonth)
                                .map(MeterReadingDto::fromEntity)
                                .orElse(null);
        }

        public Map<String, MeterReadingDto> getMeterReadingsBatch(String periodMonth, String landlordId) {
                // Get all houses owned by landlord
                List<House> houses = houseRepository.findByOwnerId(landlordId);

                // Get all room IDs from those houses
                List<String> roomIds = houses.stream()
                                .flatMap(h -> roomRepository.findByHouseId(h.getId()).stream())
                                .map(Room::getId)
                                .toList();

                if (roomIds.isEmpty()) {
                        return Map.of();
                }

                // Fetch all meter readings for these rooms in one query
                List<MeterReading> readings = meterReadingRepository.findByRoomIdInAndPeriodMonth(roomIds, periodMonth);

                // Convert to map: roomId -> MeterReadingDto
                return readings.stream()
                                .collect(Collectors.toMap(
                                                MeterReading::getRoomId,
                                                MeterReadingDto::fromEntity));
        }

        public List<InvoiceDto> getMyInvoices(String tenantId) {
                List<Invoice> invoices = invoiceRepository.findByTenantId(tenantId);
                return invoices.stream()
                                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.OVERDUE
                                                || inv.getStatus() == Invoice.InvoiceStatus.SENT
                                                || inv.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID)
                                .map(this::enrichInvoice)
                                .map(InvoiceDto::fromEntity)
                                .toList();
        }

        public InvoiceDto getMyInvoiceById(String id, String tenantId) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                if (!invoice.getTenantId().equals(tenantId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                return InvoiceDto.fromEntity(enrichInvoice(invoice));
        }

        public List<String> getContractIdsWithInvoiceForPeriod(String periodMonth) {
                return invoiceRepository.findContractIdsWithInvoiceForPeriod(periodMonth);
        }

        /**
         * Get invoice for public viewing (no auth required)
         */
        @Transactional(readOnly = true)
        public InvoiceDto getPublicInvoice(String id) {
                Invoice invoice = invoiceRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                // Only allow viewing sent/paid invoices publicly
                if (invoice.getStatus() == Invoice.InvoiceStatus.DRAFT ||
                                invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
                        throw ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND);
                }

                return InvoiceDto.fromEntity(enrichInvoice(invoice));
        }

        private Invoice enrichInvoice(Invoice invoice) {
                invoice.setItems(invoiceItemRepository.findByInvoiceId(invoice.getId()));

                User tenant = userRepository.findById(invoice.getTenantId()).orElse(null);
                if (tenant != null) {
                        invoice.setTenant(Invoice.TenantInfo.builder()
                                        .fullName(tenant.getFullName())
                                        .phone(tenant.getPhone())
                                        .build());
                }

                Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
                if (contract != null) {
                        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
                        if (room != null) {
                                House house = houseRepository.findById(room.getHouseId()).orElse(null);
                                invoice.setRoom(Invoice.RoomInfo.builder()
                                                .code(room.getCode())
                                                .houseName(house != null ? house.getName() : null)
                                                .build());
                        }
                }

                return invoice;
        }

        private void sendInvoiceNotification(Invoice invoice, Invoice enrichedInvoice) {
                NumberFormat currencyFormatter = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
                String roomCode = enrichedInvoice.getRoom() != null ? enrichedInvoice.getRoom().getCode() : "";
                String houseName = enrichedInvoice.getRoom() != null ? enrichedInvoice.getRoom().getHouseName() : "";
                String amount = currencyFormatter.format(invoice.getTotalAmount()) + " VNĐ";

                // Send notification (saves to DB + sends push)
                try {
                        String title = "Hóa đơn mới - Phòng " + roomCode;
                        String body = String.format("Bạn có hóa đơn tháng %s tại %s. Tổng tiền: %s. Hạn thanh toán: %s",
                                        invoice.getPeriodMonth(),
                                        houseName,
                                        amount,
                                        invoice.getDueDate().toString());

                        Map<String, Object> data = new HashMap<>();
                        data.put("invoiceId", invoice.getId());
                        data.put("url", "/tenant/invoices/" + invoice.getId());

                        notificationService.createNotification(
                                        invoice.getTenantId(),
                                        Notification.NotificationType.INVOICE_CREATED,
                                        title,
                                        body,
                                        data);
                } catch (Exception e) {
                        log.error("Failed to send invoice notification: {}", e.getMessage());
                }

                // Send SMS notification to primary tenant
                try {
                        Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
                        if (contract == null || contract.getRoomId() == null) {
                                log.warn("Cannot send SMS: contract or roomId is null for invoice {}", invoice.getId());
                                return;
                        }

                        // Get house address
                        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
                        House house = room != null ? houseRepository.findById(room.getHouseId()).orElse(null) : null;
                        String houseAddress = house != null ? house.getAddress() : "";

                        RoomTenant primaryTenant = roomTenantRepository.findPrimaryByRoomId(contract.getRoomId())
                                        .orElse(null);
                        if (primaryTenant == null) {
                                log.warn("Cannot send SMS: no primary tenant found for room {}", contract.getRoomId());
                                return;
                        }

                        User tenant = userRepository.findById(primaryTenant.getUserId()).orElse(null);
                        if (tenant == null || tenant.getPhone() == null) {
                                log.warn("Cannot send SMS: tenant {} has no phone number", primaryTenant.getUserId());
                                return;
                        }

                        String invoiceUrl = appBaseUrl + "/invoice/" + invoice.getId();
                        String dueDate = invoice.getDueDate().toString();
                        smsService.sendInvoiceNotification(
                                        tenant.getPhone(),
                                        roomCode,
                                        houseName,
                                        houseAddress,
                                        invoice.getPeriodMonth(),
                                        amount,
                                        dueDate,
                                        invoiceUrl);
                        log.info("SMS sent to primary tenant {} for invoice {}", tenant.getPhone(), invoice.getId());
                } catch (Exception e) {
                        log.error("Failed to send SMS for invoice {}: {}", invoice.getId(), e.getMessage());
                }
        }

        /**
         * Generate VietQR payment data for an invoice
         */
        public VietQRResponse generateVietQR(String invoiceId) {
                Invoice invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                // Only allow QR for invoices that need payment
                if (invoice.getStatus() == Invoice.InvoiceStatus.PAID ||
                                invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED ||
                                invoice.getStatus() == Invoice.InvoiceStatus.DRAFT) {
                        throw ApiException.badRequest(MessageConstant.CANNOT_GENERATE_QR);
                }

                // Get landlord from contract
                Contract contract = contractRepository.findById(invoice.getContractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                User landlord = userRepository.findById(contract.getLandlordId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.LANDLORD_NOT_FOUND));

                // Check if bank info is configured
                if (landlord.getBankCode() == null || landlord.getBankAccountNumber() == null) {
                        throw ApiException.badRequest(MessageConstant.BANK_INFO_NOT_CONFIGURED);
                }

                // Calculate remaining amount
                BigDecimal remainingAmount = invoice.getTotalAmount().subtract(invoice.getPaidAmount());

                // Get room info for description
                Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
                String roomCode = room.getCode();
                House house = houseRepository.findById(room.getHouseId()).orElse(null);

                // Simple description: Room code + period (e.g., "P101 T01/2024")
                String description = roomCode + " - " + house.getName()  + " - " +  house.getAddress() + " T"
                                + invoice.getPeriodMonth().replace("-", "/");

                // Generate VietQR URL - use qr_only template for clean QR code
                String qrUrl = String.format(
                                "https://img.vietqr.io/image/%s-%s-qr_only.png?amount=%s&addInfo=%s&accountName=%s",
                                landlord.getBankCode(),
                                landlord.getBankAccountNumber(),
                                remainingAmount.toBigInteger().toString(),
                                URLEncoder.encode(description, StandardCharsets.UTF_8),
                                URLEncoder.encode(
                                                landlord.getBankAccountHolder() != null
                                                                ? landlord.getBankAccountHolder()
                                                                : "",
                                                StandardCharsets.UTF_8));

                return com.pegacorn.rently.dto.payment.VietQRResponse.builder()
                                .qrCodeUrl(qrUrl)
                                .bankName(landlord.getBankName())
                                .bankCode(landlord.getBankCode())
                                .accountNumber(landlord.getBankAccountNumber())
                                .accountHolder(landlord.getBankAccountHolder())
                                .amount(remainingAmount)
                                .description(description)
                                .invoiceId(invoiceId)
                                .build();
        }

        /**
         * Record a manual payment (cash or bank transfer) for an invoice
         */
        @Transactional
        public com.pegacorn.rently.dto.payment.PaymentDto recordManualPayment(
                        String invoiceId,
                        BigDecimal amount,
                        String method,
                        String note,
                        String landlordId) {
                Invoice invoice = invoiceRepository.findById(invoiceId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

                Contract contract = contractRepository.findById(invoice.getContractId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

                // Verify landlord owns this invoice
                if (!contract.getLandlordId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                // Check if invoice can receive payment
                if (invoice.getStatus() == Invoice.InvoiceStatus.PAID ||
                                invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
                        throw ApiException.badRequest(MessageConstant.CANNOT_ADD_PAYMENT_TO_INVOICE);
                }

                // Determine payment method
                Payment.PaymentMethod paymentMethod;
                try {
                        paymentMethod = Payment.PaymentMethod.valueOf(method.toUpperCase());
                } catch (Exception e) {
                        paymentMethod = Payment.PaymentMethod.CASH;
                }

                // Check for existing PENDING payment - confirm it instead of creating duplicate
                List<Payment> pendingPayments = paymentRepository
                        .findByInvoiceIdAndStatus(invoiceId, Payment.PaymentStatus.PENDING);

                Payment payment;
                if (!pendingPayments.isEmpty()) {
                        // Confirm the existing pending payment
                        payment = pendingPayments.get(0);
                        payment.setStatus(Payment.PaymentStatus.SUCCESS);
                        payment.setMethod(paymentMethod);
                        payment.setNote(note);
                        payment.setPaidAt(LocalDateTime.now());
                } else {
                        // Create new payment record
                        payment = Payment.builder()
                                        .id(UUID.randomUUID().toString())
                                        .invoiceId(invoiceId)
                                        .amount(amount)
                                        .method(paymentMethod)
                                        .status(Payment.PaymentStatus.SUCCESS)
                                        .note(note)
                                        .paidAt(LocalDateTime.now())
                                        .createdAt(LocalDateTime.now())
                                        .build();
                }

                paymentRepository.save(payment);

                // Update invoice paid amount and status
                updateInvoicePaymentStatus(invoice);

                // Log activity
                Invoice enriched = enrichInvoice(invoice);
                String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "N/A";
                activityLogService.log(
                                landlordId,
                                ActivityLog.ActivityType.PAYMENT_RECORDED,
                                invoice.getId(),
                                "INVOICE",
                                "Ghi nhận thanh toán "
                                                + NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"))
                                                                .format(amount)
                                                + " VNĐ cho hóa đơn phòng " + roomCode,
                                null);

                return com.pegacorn.rently.dto.payment.PaymentDto.fromEntity(payment);
        }

        private void updateInvoicePaymentStatus(Invoice invoice) {
                List<Payment> payments = paymentRepository
                                .findByInvoiceIdAndStatus(invoice.getId(), Payment.PaymentStatus.SUCCESS);

                BigDecimal totalPaid = payments.stream()
                                .map(Payment::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                invoice.setPaidAmount(totalPaid);

                if (totalPaid.compareTo(invoice.getTotalAmount()) >= 0) {
                        invoice.setStatus(Invoice.InvoiceStatus.PAID);
                } else if (totalPaid.compareTo(BigDecimal.ZERO) > 0) {
                        invoice.setStatus(Invoice.InvoiceStatus.PARTIALLY_PAID);
                }

                invoice.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(invoice);
        }

        /**
         * Calculate utility reconciliation - compare expenses vs collected from
         * invoices
         */
        public UtilityReconciliationDto getUtilityReconciliation(String landlordId, String houseId, Integer month,
                        Integer year) {
                // Get date range for the period
                LocalDate startDate = LocalDate.of(year, month, 1);
                LocalDate endDate = startDate.plusMonths(1).minusDays(1);
                String periodMonth = String.format("%d-%02d", year, month);

                // Get category IDs for electricity and water
                String electricCategoryId = ExpenseCategoryType.ELECTRIC.getId();
                String waterCategoryId = ExpenseCategoryType.WATER.getId();

                // Get expenses for this period
                List<Expense> expenses;
                if (houseId != null) {
                        expenses = expenseRepository.findByHouseId(houseId).stream()
                                        .filter(e -> !e.getExpenseDate().isBefore(startDate)
                                                        && !e.getExpenseDate().isAfter(endDate))
                                        .toList();
                } else {
                        expenses = expenseRepository.findByLandlordId(landlordId).stream()
                                        .filter(e -> !e.getExpenseDate().isBefore(startDate)
                                                        && !e.getExpenseDate().isAfter(endDate))
                                        .toList();
                }

                // Get electricity expenses (categoryId = "electric")
                BigDecimal electricityExpense = expenses.stream()
                                .filter(e -> electricCategoryId.equals(e.getCategoryId()))
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Get water expenses (categoryId = "water")
                BigDecimal waterExpense = expenses.stream()
                                .filter(e -> waterCategoryId.equals(e.getCategoryId()))
                                .map(Expense::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Get invoice items for this period
                List<Invoice> invoices;
                if (houseId != null) {
                        invoices = invoiceRepository.findByHouseId(houseId);
                } else {
                        invoices = invoiceRepository.findByLandlordId(landlordId);
                }

                // Filter by period and exclude draft/cancelled
                invoices = invoices.stream()
                                .filter(inv -> inv.getPeriodMonth().equals(periodMonth))
                                .filter(inv -> inv.getStatus() != Invoice.InvoiceStatus.DRAFT &&
                                                inv.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                                .toList();

                // Get all invoice items for these invoices
                List<String> invoiceIds = invoices.stream().map(Invoice::getId).toList();
                List<InvoiceItem> items = invoiceItemRepository.findByInvoiceIdIn(invoiceIds);

                BigDecimal electricityCollected = items.stream()
                                .filter(item -> item.getType() == InvoiceItem.InvoiceItemType.ELECTRICITY)
                                .map(InvoiceItem::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                BigDecimal waterCollected = items.stream()
                                .filter(item -> item.getType() == InvoiceItem.InvoiceItemType.WATER)
                                .map(InvoiceItem::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Count previously applied shortfalls (items with type OTHER and specific
                // description)
                String shortfallDescPrefix = "Bù điện nước tháng " + periodMonth;
                BigDecimal alreadyCompensated = items.stream()
                                .filter(item -> item.getType() == InvoiceItem.InvoiceItemType.OTHER &&
                                                item.getDescription() != null &&
                                                item.getDescription().startsWith(shortfallDescPrefix))
                                .map(InvoiceItem::getAmount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                // Calculate shortfalls
                BigDecimal electricityShortfall = electricityExpense.subtract(electricityCollected)
                                .max(BigDecimal.ZERO);
                BigDecimal waterShortfall = waterExpense.subtract(waterCollected).max(BigDecimal.ZERO);

                // Total shortfall should reduce by what was already compensated
                BigDecimal totalGap = electricityShortfall.add(waterShortfall);
                BigDecimal remainingShortfall = totalGap.subtract(alreadyCompensated).max(BigDecimal.ZERO);

                // Get room count for splitting - all RENTED rooms
                List<Room> rooms;
                if (houseId != null) {
                        rooms = roomRepository.findByHouseId(houseId);
                } else {
                        List<House> houses = houseRepository.findByOwnerId(landlordId);
                        rooms = houses.stream()
                                        .flatMap(h -> roomRepository.findByHouseId(h.getId()).stream())
                                        .toList();
                }
                int activeRoomCount = (int) rooms.stream()
                                .filter(r -> contractRepository.findByRoomIdAndStatus(r.getId(), Contract.ContractStatus.ACTIVE).isPresent())
                                .count();

                // Calculate per room amounts based on REMAINING shortfall
                BigDecimal electricityPerRoom = BigDecimal.ZERO;
                BigDecimal waterPerRoom = BigDecimal.ZERO;

                if (remainingShortfall.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal originalTotal = electricityShortfall.add(waterShortfall);
                        if (originalTotal.compareTo(BigDecimal.ZERO) > 0) {
                                BigDecimal elecRatio = electricityShortfall.divide(originalTotal, 10,
                                                RoundingMode.HALF_UP);
                                BigDecimal remainingElec = remainingShortfall.multiply(elecRatio);
                                BigDecimal remainingWater = remainingShortfall.subtract(remainingElec);

                                electricityPerRoom = activeRoomCount > 0
                                                ? remainingElec.divide(BigDecimal.valueOf(activeRoomCount), 0,
                                                                RoundingMode.CEILING)
                                                : BigDecimal.ZERO;

                                waterPerRoom = activeRoomCount > 0
                                                ? remainingWater.divide(BigDecimal.valueOf(activeRoomCount), 0,
                                                                RoundingMode.CEILING)
                                                : BigDecimal.ZERO;
                        }
                }

                // Build category breakdown list for all expense categories
                List<UtilityReconciliationDto.CategoryBreakdown> categories = buildCategoryBreakdowns(
                                expenses, items, activeRoomCount);

                return new UtilityReconciliationDto(
                                electricityExpense,
                                electricityCollected,
                                electricityShortfall,
                                electricityPerRoom,
                                waterExpense,
                                waterCollected,
                                waterShortfall,
                                waterPerRoom,
                                remainingShortfall,
                                activeRoomCount,
                                categories);
        }

        /**
         * Build category breakdown list for reconciliation display
         */
        private List<UtilityReconciliationDto.CategoryBreakdown> buildCategoryBreakdowns(
                        List<Expense> expenses, List<InvoiceItem> items, int activeRoomCount) {
                List<UtilityReconciliationDto.CategoryBreakdown> breakdowns = new java.util.ArrayList<>();

                // Map invoice item types to expense categories
                java.util.Map<String, InvoiceItem.InvoiceItemType> categoryToItemType = java.util.Map.of(
                                ExpenseCategoryType.ELECTRIC.getId(), InvoiceItem.InvoiceItemType.ELECTRICITY,
                                ExpenseCategoryType.WATER.getId(), InvoiceItem.InvoiceItemType.WATER);

                // Process each expense category
                for (ExpenseCategoryType category : ExpenseCategoryType.values()) {
                        // Sum expenses for this category
                        BigDecimal expense = expenses.stream()
                                        .filter(e -> category.getId().equals(e.getCategoryId()))
                                        .map(Expense::getAmount)
                                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                        // Sum collected from invoices
                        BigDecimal collected = BigDecimal.ZERO;
                        InvoiceItem.InvoiceItemType itemType = categoryToItemType.get(category.getId());
                        if (itemType != null) {
                                // Direct mapping (electricity, water)
                                collected = items.stream()
                                                .filter(item -> item.getType() == itemType)
                                                .map(InvoiceItem::getAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        } else {
                                // For SERVICE items, match by description containing category name
                                String categoryName = category.getNameVi().toLowerCase();
                                collected = items.stream()
                                                .filter(item -> item.getType() == InvoiceItem.InvoiceItemType.SERVICE &&
                                                                item.getDescription() != null &&
                                                                item.getDescription().toLowerCase().contains(categoryName))
                                                .map(InvoiceItem::getAmount)
                                                .reduce(BigDecimal.ZERO, BigDecimal::add);
                        }

                        // Calculate shortfall (only positive values)
                        BigDecimal shortfall = expense.subtract(collected).max(BigDecimal.ZERO);

                        // Calculate per room
                        BigDecimal perRoom = BigDecimal.ZERO;
                        if (shortfall.compareTo(BigDecimal.ZERO) > 0 && activeRoomCount > 0) {
                                perRoom = shortfall.divide(BigDecimal.valueOf(activeRoomCount), 0, RoundingMode.CEILING);
                        }

                        // Only include categories that have either expense or collected amount
                        if (expense.compareTo(BigDecimal.ZERO) > 0 || collected.compareTo(BigDecimal.ZERO) > 0) {
                                breakdowns.add(new UtilityReconciliationDto.CategoryBreakdown(
                                                category.getId(),
                                                category.getNameVi(),
                                                category.getIcon(),
                                                expense,
                                                collected,
                                                shortfall,
                                                perRoom));
                        }
                }

                return breakdowns;
        }

        /**
         * Flag utility shortfall for next month's invoices
         */
        @Transactional
        public UtilityShortfall flagShortfall(String landlordId, String houseId, Integer month, Integer year) {
                // Verify house ownership
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.NOT_AUTHORIZED);
                }

                String periodMonth = String.format("%d-%02d", year, month);

                // Check if already flagged for this period
                var existing = utilityShortfallRepository.findByHouseIdAndPeriodMonth(houseId, periodMonth);
                if (existing.isPresent()) {
                        throw ApiException.conflict(MessageConstant.SHORTFALL_ALREADY_FLAGGED);
                }

                // Get reconciliation data
                UtilityReconciliationDto reconciliation = getUtilityReconciliation(landlordId, houseId, month, year);

                if (reconciliation.totalShortfall().compareTo(BigDecimal.ZERO) <= 0) {
                        throw ApiException.badRequest(MessageConstant.NO_SHORTFALL_TO_FLAG);
                }

                // Create shortfall record
                UtilityShortfall shortfall = UtilityShortfall.builder()
                                .id(UUID.randomUUID().toString())
                                .houseId(houseId)
                                .periodMonth(periodMonth)
                                .electricityShortfall(reconciliation.electricityShortfall())
                                .waterShortfall(reconciliation.waterShortfall())
                                .totalShortfall(reconciliation.totalShortfall())
                                .perRoomAmount(reconciliation.activeRoomCount() > 0
                                                ? reconciliation.totalShortfall().divide(
                                                                BigDecimal.valueOf(reconciliation.activeRoomCount()),
                                                                0,
                                                                RoundingMode.CEILING)
                                                : BigDecimal.ZERO)
                                .activeRoomCount(reconciliation.activeRoomCount())
                                .status(UtilityShortfall.Status.PENDING)
                                .createdAt(LocalDateTime.now())
                                .build();

                return utilityShortfallRepository.save(shortfall);
        }

        /**
         * Get pending shortfalls for a house
         */
        public List<UtilityShortfall> getPendingShortfalls(String landlordId, String houseId) {
                // Verify house ownership
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.NOT_AUTHORIZED);
                }

                return utilityShortfallRepository.findByHouseIdAndStatus(houseId, UtilityShortfall.Status.PENDING);
        }

        /**
         * Mark shortfall as applied
         */
        @Transactional
        public UtilityShortfall markShortfallApplied(String landlordId, String shortfallId) {
                UtilityShortfall shortfall = utilityShortfallRepository.findById(shortfallId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.SHORTFALL_NOT_FOUND));

                // Verify house ownership
                House house = houseRepository.findById(shortfall.getHouseId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.NOT_AUTHORIZED);
                }

                shortfall.setStatus(UtilityShortfall.Status.APPLIED);
                shortfall.setAppliedAt(LocalDateTime.now());

                return utilityShortfallRepository.save(shortfall);
        }

        /**
         * Delete a pending shortfall (unflag)
         */
        @Transactional
        public void deleteShortfall(String landlordId, String shortfallId) {
                UtilityShortfall shortfall = utilityShortfallRepository.findById(shortfallId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.SHORTFALL_NOT_FOUND));

                // Verify house ownership
                House house = houseRepository.findById(shortfall.getHouseId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.NOT_AUTHORIZED);
                }

                if (shortfall.getStatus() != UtilityShortfall.Status.PENDING) {
                        throw ApiException.badRequest(MessageConstant.ONLY_DELETE_PENDING_SHORTFALL);
                }

                utilityShortfallRepository.delete(shortfall);
        }

        /**
         * Apply shortfall to invoices - either add to existing drafts or create new
         * invoices
         * Returns the number of invoices affected
         */
        @Transactional
        public int applyShortfallToInvoices(String landlordId, String houseId, Integer month, Integer year) {
                // Verify house ownership
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.NOT_AUTHORIZED);
                }

                String periodMonth = String.format("%d-%02d", year, month);

                // Get reconciliation to calculate shortfall
                // Note: totalShortfall() now returns the REMAINING shortfall (after subtracting
                // existing make-up items)
                UtilityReconciliationDto reconciliation = getUtilityReconciliation(landlordId, houseId, month, year);

                // If remaining is 0, nothing to do
                if (reconciliation.totalShortfall().compareTo(BigDecimal.ZERO) <= 0) {
                        throw ApiException.badRequest(MessageConstant.NO_SHORTFALL_TO_APPLY);
                }

                // Calculate per-room amount using RAW shortfall (Activite + Water) to ensure
                // consistent charging
                // If we used "Remaining", the charge would dilute as fewer people are left to
                // pay
                BigDecimal rawTotalShortfall = reconciliation.electricityShortfall()
                                .add(reconciliation.waterShortfall());
                BigDecimal perRoomAmount = reconciliation.activeRoomCount() > 0
                                ? rawTotalShortfall.divide(
                                                BigDecimal.valueOf(reconciliation.activeRoomCount()),
                                                0,
                                                RoundingMode.CEILING)
                                : BigDecimal.ZERO;

                // Get all rooms in this house with active contracts
                List<Room> rooms = roomRepository.findByHouseId(houseId).stream()
                                .filter(r -> r.getStatus() == Room.RoomStatus.RENTED)
                                .toList();

                // Get invoices for this house and period
                List<Invoice> invoices = invoiceRepository.findByHouseId(houseId).stream()
                                .filter(inv -> inv.getPeriodMonth().equals(periodMonth))
                                .filter(inv -> inv.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                                .toList();

                int count = 0;
                String description = "Bù điện nước tháng " + periodMonth;

                for (Room room : rooms) {
                        // Find contract for this room
                        Contract contract = contractRepository
                                        .findByRoomIdAndStatus(room.getId(), Contract.ContractStatus.ACTIVE)
                                        .orElse(null);
                        if (contract == null)
                                continue;

                        // Find existing invoice for this contract and period
                        Invoice existingInvoice = invoices.stream()
                                        .filter(inv -> inv.getContractId().equals(contract.getId()))
                                        .findFirst()
                                        .orElse(null);

                        // Check if THIS specific room/invoice already has the make-up item
                        boolean alreadyApplied = false;
                        if (existingInvoice != null) {
                                List<InvoiceItem> items = invoiceItemRepository
                                                .findByInvoiceId(existingInvoice.getId());
                                alreadyApplied = items.stream()
                                                .anyMatch(item -> item.getType() == InvoiceItem.InvoiceItemType.OTHER &&
                                                                description.equals(item.getDescription()));
                        }

                        if (alreadyApplied) {
                                continue; // Skip this room, it's already charged
                        }

                        if (existingInvoice != null && existingInvoice.getStatus() == Invoice.InvoiceStatus.DRAFT) {
                                // Add item to existing draft invoice
                                addItemToInvoice(existingInvoice, description, perRoomAmount);
                                count++;
                        } else if (existingInvoice == null || existingInvoice.getStatus() == Invoice.InvoiceStatus.PAID
                                        ||
                                        existingInvoice.getStatus() == Invoice.InvoiceStatus.SENT ||
                                        existingInvoice.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID) {
                                // Create new invoice just for shortfall
                                createShortfallInvoice(contract, periodMonth, description, perRoomAmount);
                                count++;
                        }
                }

                // Mark pending UtilityShortfall as APPLIED if found
                utilityShortfallRepository.findByHouseIdAndStatus(houseId, UtilityShortfall.Status.PENDING).stream()
                                .filter(us -> us.getPeriodMonth().equals(periodMonth))
                                .forEach(shortfall -> {
                                        shortfall.setStatus(UtilityShortfall.Status.APPLIED);
                                        shortfall.setAppliedAt(LocalDateTime.now());
                                        utilityShortfallRepository.save(shortfall);
                                });

                return count;
        }

        /**
         * Add an item to an existing DRAFT invoice
         */
        private void addItemToInvoice(Invoice invoice, String description, BigDecimal amount) {
                InvoiceItem item = InvoiceItem.builder()
                                .id(UUID.randomUUID().toString())
                                .invoiceId(invoice.getId())
                                .type(InvoiceItem.InvoiceItemType.OTHER)
                                .description(description)
                                .quantity(BigDecimal.ONE)
                                .unitPrice(amount)
                                .amount(amount)
                                .createdAt(LocalDateTime.now())
                                .build();
                invoiceItemRepository.save(item);

                // Update invoice total
                invoice.setTotalAmount(invoice.getTotalAmount().add(amount));
                invoice.setUpdatedAt(LocalDateTime.now());
                invoiceRepository.save(invoice);
        }

        /**
         * Create a new invoice just for the shortfall amount
         */
        private void createShortfallInvoice(Contract contract, String periodMonth, String description,
                        BigDecimal amount) {
                // Due date is 5 days from now
                LocalDate dueDate = LocalDate.now().plusDays(5);

                Invoice invoice = Invoice.builder()
                                .id(UUID.randomUUID().toString())
                                .contractId(contract.getId())
                                .tenantId(contract.getTenantId())
                                .periodMonth(periodMonth)
                                .dueDate(dueDate)
                                .totalAmount(amount)
                                .paidAmount(BigDecimal.ZERO)
                                .lateFeePercent(BigDecimal.ZERO)
                                .status(Invoice.InvoiceStatus.DRAFT)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();
                invoiceRepository.save(invoice);

                InvoiceItem item = InvoiceItem.builder()
                                .id(UUID.randomUUID().toString())
                                .invoiceId(invoice.getId())
                                .type(InvoiceItem.InvoiceItemType.OTHER)
                                .description(description)
                                .quantity(BigDecimal.ONE)
                                .unitPrice(amount)
                                .amount(amount)
                                .createdAt(LocalDateTime.now())
                                .build();
                invoiceItemRepository.save(item);
        }

        /**
         * Get invoice status summary for a house and period
         */
        public Map<String, Object> getInvoiceStatusForPeriod(String landlordId, String houseId, Integer month,
                        Integer year) {
                // Verify house ownership
                House house = houseRepository.findById(houseId)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.NOT_AUTHORIZED);
                }

                String periodMonth = String.format("%d-%02d", year, month);

                List<Invoice> invoices = invoiceRepository.findByHouseId(houseId).stream()
                                .filter(inv -> inv.getPeriodMonth().equals(periodMonth))
                                .filter(inv -> inv.getStatus() != Invoice.InvoiceStatus.CANCELLED)
                                .toList();

                int draftCount = (int) invoices.stream().filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.DRAFT)
                                .count();
                int sentCount = (int) invoices.stream().filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.SENT)
                                .count();
                int paidCount = (int) invoices.stream().filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PAID)
                                .count();
                int partiallyPaidCount = (int) invoices.stream()
                                .filter(inv -> inv.getStatus() == Invoice.InvoiceStatus.PARTIALLY_PAID).count();

                Map<String, Object> result = new HashMap<>();
                result.put("totalCount", invoices.size());
                result.put("draftCount", draftCount);
                result.put("sentCount", sentCount);
                result.put("paidCount", paidCount);
                result.put("partiallyPaidCount", partiallyPaidCount);
                result.put("hasDrafts", draftCount > 0);
                result.put("allPaidOrSent", draftCount == 0 && !invoices.isEmpty());

                return result;
        }

        /**
         * Create ADJUSTMENT invoice from reconciliation diffs.
         * Supports 3 modes: POSITIVE_ONLY, NEGATIVE_ONLY, NET
         */
        @Transactional
        public InvoiceDto createAdjustmentInvoice(String landlordId, CreateAdjustmentDto dto) {
                // Verify house ownership
                House house = houseRepository.findById(dto.houseId())
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
                if (!house.getOwnerId().equals(landlordId)) {
                        throw ApiException.forbidden(MessageConstant.NOT_AUTHORIZED);
                }

                List<CreateAdjustmentDto.DiffItem> selectedDiffs = dto.diffs();
                if (selectedDiffs == null || selectedDiffs.isEmpty()) {
                        throw ApiException.badRequest("Chưa chọn khoản chênh lệch nào");
                }

                // Validate according to mode
                switch (dto.mode()) {
                        case POSITIVE_ONLY:
                                if (selectedDiffs.stream().anyMatch(d -> d.amount().compareTo(BigDecimal.ZERO) <= 0)) {
                                        throw ApiException.badRequest("Chế độ thu thêm chỉ cho phép khoản dương");
                                }
                                break;
                        case NEGATIVE_ONLY:
                                if (selectedDiffs.stream().anyMatch(d -> d.amount().compareTo(BigDecimal.ZERO) >= 0)) {
                                        throw ApiException.badRequest("Chế độ hoàn tiền chỉ cho phép khoản âm");
                                }
                                break;
                        case NET:
                                // Allow mixing positive and negative
                                break;
                }

                // Calculate total
                BigDecimal total = selectedDiffs.stream()
                                .map(CreateAdjustmentDto.DiffItem::amount)
                                .reduce(BigDecimal.ZERO, BigDecimal::add);

                if (total.compareTo(BigDecimal.ZERO) == 0 && dto.mode() == CreateAdjustmentDto.AdjustmentMode.NET) {
                        throw ApiException.badRequest("Tổng bù trừ = 0, không cần tạo hóa đơn");
                }

                // Get rooms with ACTIVE contracts (not just RENTED status)
                // This ensures we only count rooms that will actually receive invoices
                List<Room> rentedRooms = roomRepository.findByHouseId(dto.houseId()).stream()
                                .filter(r -> r.getStatus() == Room.RoomStatus.RENTED)
                                .filter(r -> contractRepository.findByRoomIdAndStatus(r.getId(), Contract.ContractStatus.ACTIVE).isPresent())
                                .toList();

                if (rentedRooms.isEmpty()) {
                        throw ApiException.badRequest("Không có phòng nào có hợp đồng đang hoạt động để tạo hóa đơn");
                }

                // Divide total by number of rooms with active contracts
                int activeRoomCount = rentedRooms.size();
                BigDecimal perRoomAmount = total.divide(BigDecimal.valueOf(activeRoomCount), 0, RoundingMode.CEILING);

                LocalDate dueDate = LocalDate.now().plusDays(dto.dueDays() != null ? dto.dueDays() : 5);
                boolean isNetting = dto.mode() == CreateAdjustmentDto.AdjustmentMode.NET;
                String description = isNetting ? "Bù trừ chênh lệch dịch vụ"
                                : (total.compareTo(BigDecimal.ZERO) > 0 ? "Thu thêm dịch vụ" : "Hoàn tiền dịch vụ");

                int createdCount = 0;
                Invoice lastCreated = null;

                for (Room room : rentedRooms) {
                        Contract contract = contractRepository
                                        .findByRoomIdAndStatus(room.getId(), Contract.ContractStatus.ACTIVE)
                                        .orElse(null);
                        if (contract == null)
                                continue;

                        String invoiceId = UUID.randomUUID().toString();
                        Invoice invoice = Invoice.builder()
                                        .id(invoiceId)
                                        .contractId(contract.getId())
                                        .tenantId(contract.getTenantId())
                                        .periodMonth(dto.periodMonth())
                                        .dueDate(dueDate)
                                        .totalAmount(perRoomAmount)
                                        .paidAmount(BigDecimal.ZERO)
                                        .lateFeePercent(BigDecimal.ZERO)
                                        .status(Invoice.InvoiceStatus.DRAFT)
                                        .invoiceType(Invoice.InvoiceType.ADJUSTMENT)
                                        .isNetting(isNetting)
                                        .createdAt(LocalDateTime.now())
                                        .updatedAt(LocalDateTime.now())
                                        .build();

                        invoiceRepository.save(invoice);

                        // Create invoice items
                        for (CreateAdjustmentDto.DiffItem diff : selectedDiffs) {
                                BigDecimal itemPerRoom = diff.amount().divide(BigDecimal.valueOf(activeRoomCount), 0,
                                                RoundingMode.CEILING);
                                InvoiceItem item = InvoiceItem.builder()
                                                .id(UUID.randomUUID().toString())
                                                .invoiceId(invoiceId)
                                                .type(InvoiceItem.InvoiceItemType.OTHER)
                                                .description(description + " - " + diff.serviceName())
                                                .quantity(BigDecimal.ONE)
                                                .unitPrice(itemPerRoom)
                                                .amount(itemPerRoom)
                                                .createdAt(LocalDateTime.now())
                                                .build();
                                invoiceItemRepository.save(item);
                        }

                        lastCreated = invoice;
                        createdCount++;
                }

                if (createdCount == 0) {
                        throw ApiException.badRequest("Không có hợp đồng hoạt động để tạo hóa đơn");
                }

                return InvoiceDto.fromEntity(enrichInvoice(lastCreated));
        }
}
