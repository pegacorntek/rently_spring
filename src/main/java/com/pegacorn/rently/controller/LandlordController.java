package com.pegacorn.rently.controller;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.activity.ActivityLogDto;
import com.pegacorn.rently.dto.amenity.*;
import com.pegacorn.rently.dto.common.ApiResponse;
import com.pegacorn.rently.dto.contract.ContractDto;
import com.pegacorn.rently.dto.contract.ContractSnapshotDto;
import com.pegacorn.rently.dto.contract.CreateContractRequest;
import com.pegacorn.rently.dto.contract.UpdateContractRequest;
import com.pegacorn.rently.dto.contracttemplate.ContractTemplateDto;
import com.pegacorn.rently.dto.contracttemplate.CreateContractTemplateRequest;
import com.pegacorn.rently.dto.contracttemplate.UpdateContractTemplateRequest;
import com.pegacorn.rently.dto.expense.*;
import com.pegacorn.rently.dto.house.CreateHouseRequest;
import com.pegacorn.rently.dto.auth.OtpRequest;
import com.pegacorn.rently.dto.auth.UserDto;
import com.pegacorn.rently.dto.auth.VerifyOtpRequest;
import com.pegacorn.rently.entity.OtpVerification;
import com.pegacorn.rently.dto.house.HouseDto;
import com.pegacorn.rently.dto.house.HouseStatsDto;
import com.pegacorn.rently.dto.house.UpdateHouseRequest;
import com.pegacorn.rently.dto.invoice.*;
import com.pegacorn.rently.dto.ocr.ScanContractResponse;
import com.pegacorn.rently.dto.payment.CashPaymentRequest;
import com.pegacorn.rently.dto.payment.PaymentDto;
import com.pegacorn.rently.dto.room.*;
import com.pegacorn.rently.dto.servicefee.CreateServiceFeeRequest;
import com.pegacorn.rently.dto.servicefee.ServiceFeeDto;
import com.pegacorn.rently.dto.servicefee.UpdateServiceFeeRequest;
import com.pegacorn.rently.dto.landlord.DashboardSummaryDto;
import com.pegacorn.rently.dto.task.CreateTaskRequest;
import com.pegacorn.rently.dto.task.TaskDto;
import com.pegacorn.rently.dto.tenant.TenantListDto;
import com.pegacorn.rently.dto.tenant.UpdateTenantRequest;
import com.pegacorn.rently.dto.ticket.TicketDto;
import com.pegacorn.rently.dto.ticket.UpdateTicketRequest;
import com.pegacorn.rently.security.UserPrincipal;
import com.pegacorn.rently.service.*;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/landlord")
@RequiredArgsConstructor
@Slf4j
public class LandlordController {

    private final ActivityLogService activityLogService;
    private final AmenityService amenityService;
    private final ContractService contractService;
    private final ContractTemplateService templateService;
    private final ExpenseService expenseService;
    private final HouseService houseService;
    private final InvoiceService invoiceService;
    private final OcrService ocrService;
    private final PaymentService paymentService;
    private final RoomService roomService;
    private final ServiceFeeService serviceFeeService;
    private final TaskService taskService;
    private final TenantService tenantService;
    private final TicketService ticketService;
    private final AuthService authService;
    private final DashboardService dashboardService;

    // ==================== DASHBOARD ====================

    @GetMapping("/dashboard-summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getDashboardSummary(
            @RequestParam(required = false) String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        DashboardSummaryDto summary = dashboardService.getSummary(houseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ==================== ACTIVITIES ====================

    @GetMapping("/activities")
    public ResponseEntity<ApiResponse<List<ActivityLogDto>>> getRecentActivities(
            @RequestParam(defaultValue = "10") int limit,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ActivityLogDto> activities = activityLogService.getRecentActivities(principal.getId(), limit);
        return ResponseEntity.ok(ApiResponse.success(activities));
    }

    // ==================== BADGE COUNTS ====================

    @GetMapping("/badge-counts")
    public ResponseEntity<ApiResponse<com.pegacorn.rently.dto.common.BadgeCountsDto>> getBadgeCounts(
            @RequestParam(required = false) String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Count draft contracts
        int draftContractsCount = (int) contractService.getAllByLandlord(principal.getId(), houseId, "DRAFT").size();

        // Count unresolved tickets (OPEN or IN_PROGRESS)
        var allTickets = ticketService.getAllByLandlord(principal.getId(), houseId, null);
        int unresolvedTicketsCount = (int) allTickets.stream()
                .filter(t -> "OPEN".equals(t.status()) || "IN_PROGRESS".equals(t.status()))
                .count();

        // Count unpaid invoices (only after billing day - 25th)
        int unpaidInvoicesCount = 0;
        java.time.LocalDate today = java.time.LocalDate.now();
        if (today.getDayOfMonth() > 25) {
            var allInvoices = invoiceService.getAllByLandlord(principal.getId(), houseId, null, null, null);
            unpaidInvoicesCount = (int) allInvoices.stream()
                    .filter(inv -> "DRAFT".equals(inv.status()) || "PARTIALLY_PAID".equals(inv.status())
                            || "OVERDUE".equals(inv.status()))
                    .count();
        }

        // Check for utility shortfall
        boolean hasShortfall = false;
        if (houseId != null) {
            try {
                var reconciliation = invoiceService.getUtilityReconciliation(
                        principal.getId(), houseId, today.getMonthValue(), today.getYear());
                hasShortfall = reconciliation.totalShortfall().compareTo(java.math.BigDecimal.ZERO) > 0;
            } catch (Exception e) {
                // Ignore - no shortfall data
            }
        }

        // Check if there are any rented rooms
        boolean hasRentedRooms = false;
        if (houseId != null) {
            var rooms = roomService.getByHouse(houseId, principal.getId());
            hasRentedRooms = rooms.stream().anyMatch(r -> "RENTED".equals(r.status()));
        }

        // Count undone tasks
        var allTasks = taskService.getAll(principal.getId());
        int undoneTasksCount = (int) allTasks.stream().filter(t -> !t.isDone()).count();

        var badgeCounts = new com.pegacorn.rently.dto.common.BadgeCountsDto(
                draftContractsCount,
                unresolvedTicketsCount,
                unpaidInvoicesCount,
                hasShortfall,
                hasRentedRooms,
                undoneTasksCount);

        return ResponseEntity.ok(ApiResponse.success(badgeCounts));
    }

    // ==================== HOUSES ====================

    @GetMapping("/houses")
    public ResponseEntity<ApiResponse<List<HouseDto>>> getAllHouses(@AuthenticationPrincipal UserPrincipal principal) {
        List<HouseDto> houses = houseService.getAllByOwner(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(houses));
    }

    @GetMapping("/houses/{id}")
    public ResponseEntity<ApiResponse<HouseDto>> getHouseById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        HouseDto house = houseService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(house));
    }

    @PostMapping("/houses")
    public ResponseEntity<ApiResponse<HouseDto>> createHouse(
            @Valid @RequestBody CreateHouseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        HouseDto house = houseService.create(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(house, MessageConstant.HOUSE_CREATED_SUCCESS));
    }

    @PutMapping("/houses/{id}")
    public ResponseEntity<ApiResponse<HouseDto>> updateHouse(
            @PathVariable String id,
            @Valid @RequestBody UpdateHouseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        HouseDto house = houseService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(house, MessageConstant.HOUSE_UPDATED_SUCCESS));
    }

    @DeleteMapping("/houses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteHouse(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        houseService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.HOUSE_DELETED_SUCCESS));
    }

    @GetMapping("/houses/stats")
    public ResponseEntity<ApiResponse<List<HouseStatsDto>>> getHouseStats(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {
        List<HouseStatsDto> stats = houseService.getStats(principal.getId(), month, year);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    @GetMapping("/rooms")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getAllRooms(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RoomDto> rooms = roomService.getAll(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    // ==================== ROOMS ====================

    @GetMapping("/houses/{houseId}/rooms")
    public ResponseEntity<ApiResponse<List<RoomDto>>> getRoomsByHouse(
            @PathVariable String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RoomDto> rooms = roomService.getByHouse(houseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/houses/{houseId}/rooms/paginated")
    public ResponseEntity<ApiResponse<Page<RoomDto>>> getRoomsByHousePaginated(
            @PathVariable String houseId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal UserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        Page<RoomDto> rooms = roomService.getByHousePaginated(houseId, principal.getId(), pageable);
        return ResponseEntity.ok(ApiResponse.success(rooms));
    }

    @GetMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomDto>> getRoomById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoomDto room = roomService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(room));
    }

    @PostMapping("/rooms")
    public ResponseEntity<ApiResponse<RoomDto>> createRoom(
            @Valid @RequestBody CreateRoomRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoomDto room = roomService.create(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(room, MessageConstant.ROOM_CREATED_SUCCESS));
    }

    @PostMapping("/rooms/batch")
    public ResponseEntity<ApiResponse<BatchCreateRoomResponse>> createRoomsBatch(
            @Valid @RequestBody BatchCreateRoomRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        BatchCreateRoomResponse response = roomService.createBatch(request, principal.getId());
        String message = String.format("Đã tạo %d/%d phòng", response.successCount(), response.totalRequested());
        return ResponseEntity.ok(ApiResponse.success(response, message));
    }

    @PutMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<RoomDto>> updateRoom(
            @PathVariable String id,
            @Valid @RequestBody UpdateRoomRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoomDto room = roomService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(room, MessageConstant.ROOM_UPDATED_SUCCESS));
    }

    @DeleteMapping("/rooms/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteRoom(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.ROOM_DELETED_SUCCESS));
    }

    @PostMapping("/rooms/{roomId}/tenants")
    public ResponseEntity<ApiResponse<Void>> addTenantToRoom(
            @PathVariable String roomId,
            @Valid @RequestBody AddTenantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomService.addTenant(roomId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.TENANT_ADDED_SUCCESS));
    }

    @DeleteMapping("/rooms/{roomId}/tenants/{tenantId}")
    public ResponseEntity<ApiResponse<Void>> removeTenantFromRoom(
            @PathVariable String roomId,
            @PathVariable String tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomService.removeTenant(roomId, tenantId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.TENANT_REMOVED_SUCCESS));
    }

    @PutMapping("/rooms/{roomId}/tenants/{tenantId}/primary")
    public ResponseEntity<ApiResponse<Void>> setPrimaryTenant(
            @PathVariable String roomId,
            @PathVariable String tenantId,
            @AuthenticationPrincipal UserPrincipal principal) {
        roomService.setPrimaryTenant(roomId, tenantId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.PRIMARY_TENANT_UPDATED_SUCCESS));
    }

    // ==================== TENANTS ====================

    @GetMapping("/tenants")
    public ResponseEntity<ApiResponse<Page<TenantListDto>>> getAllTenants(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Pageable pageable = PageRequest.of(page, size);
        Page<TenantListDto> tenants = tenantService.getAllTenantsForLandlord(principal.getId(), houseId, search, pageable);
        return ResponseEntity.ok(ApiResponse.success(tenants));
    }

    @PutMapping("/tenants/{id}")
    public ResponseEntity<ApiResponse<TenantListDto>> updateTenant(
            @PathVariable String id,
            @Valid @RequestBody UpdateTenantRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TenantListDto tenant = tenantService.updateTenant(id, principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(tenant));
    }

    // ==================== CONTRACTS ====================

    @GetMapping("/contracts")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getAllContracts(
            @RequestParam(required = false) String houseId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ContractDto> contracts = contractService.getAllByLandlord(principal.getId(), houseId, status);
        return ResponseEntity.ok(ApiResponse.success(contracts));
    }

    @GetMapping("/contracts/{id}")
    public ResponseEntity<ApiResponse<ContractDto>> getContractById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractDto contract = contractService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(contract));
    }

    @PostMapping("/contracts")
    public ResponseEntity<ApiResponse<ContractDto>> createContract(
            @Valid @RequestBody CreateContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractDto contract = contractService.create(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(contract, MessageConstant.CONTRACT_CREATED_SUCCESS));
    }

    @PutMapping("/contracts/{id}")
    public ResponseEntity<ApiResponse<ContractDto>> updateContract(
            @PathVariable String id,
            @Valid @RequestBody UpdateContractRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractDto contract = contractService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(contract, MessageConstant.CONTRACT_UPDATED_SUCCESS));
    }

    @PutMapping("/contracts/{id}/activate")
    public ResponseEntity<ApiResponse<ContractDto>> activateContract(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractDto contract = contractService.activate(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(contract, MessageConstant.CONTRACT_ACTIVATED_SUCCESS));
    }

    @PutMapping("/contracts/{id}/end")
    public ResponseEntity<ApiResponse<ContractDto>> endContract(
            @PathVariable String id,
            @RequestParam(defaultValue = "false") boolean removeAllTenants,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractDto contract = contractService.end(id, principal.getId(), removeAllTenants);
        return ResponseEntity.ok(ApiResponse.success(contract, MessageConstant.CONTRACT_ENDED_SUCCESS));
    }

    @DeleteMapping("/contracts/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContract(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        contractService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.CONTRACT_DELETED_SUCCESS));
    }

    @PutMapping("/contracts/{id}/confirm-deposit")
    public ResponseEntity<ApiResponse<ContractDto>> confirmContractDeposit(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractDto contract = contractService.confirmDeposit(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(contract, MessageConstant.DEPOSIT_CONFIRMED_SUCCESS));
    }

    @GetMapping("/contracts/{id}/render")
    public ResponseEntity<ApiResponse<String>> renderContractContent(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        String html = contractService.renderContent(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(html));
    }

    @PutMapping("/contracts/{id}/refresh-snapshot")
    public ResponseEntity<ApiResponse<ContractDto>> refreshContractSnapshot(
            @PathVariable String id,
            @RequestParam(required = false) String changeNote,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractDto contract = contractService.refreshSnapshot(id, principal.getId(), changeNote);
        return ResponseEntity.ok(ApiResponse.success(contract, MessageConstant.CONTRACT_SNAPSHOT_REFRESHED_SUCCESS));
    }

    @GetMapping("/contracts/deposits")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getContractDeposits(
            @RequestParam(required = false) String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ContractDto> contracts = contractService.getDeposits(principal.getId(), houseId);
        return ResponseEntity.ok(ApiResponse.success(contracts));
    }

    @GetMapping("/contracts/{id}/history")
    public ResponseEntity<ApiResponse<List<ContractSnapshotDto>>> getContractHistory(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ContractSnapshotDto> snapshots = contractService.getSnapshotHistory(id, principal.getId())
                .stream()
                .map(ContractSnapshotDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(snapshots));
    }

    // ==================== CONTRACT TEMPLATES ====================

    @GetMapping("/contract-templates")
    public ResponseEntity<ApiResponse<List<ContractTemplateDto>>> getAllContractTemplates(
            @RequestParam(required = false) String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ContractTemplateDto> templates = templateService.getAll(principal.getId(), houseId);
        return ResponseEntity.ok(ApiResponse.success(templates));
    }

    @GetMapping("/contract-templates/{id}")
    public ResponseEntity<ApiResponse<ContractTemplateDto>> getContractTemplateById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractTemplateDto template = templateService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(template));
    }

    @PostMapping("/contract-templates")
    public ResponseEntity<ApiResponse<ContractTemplateDto>> createContractTemplate(
            @Valid @RequestBody CreateContractTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractTemplateDto template = templateService.create(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(template, MessageConstant.CONTRACT_TEMPLATE_CREATED_SUCCESS));
    }

    @PutMapping("/contract-templates/{id}")
    public ResponseEntity<ApiResponse<ContractTemplateDto>> updateContractTemplate(
            @PathVariable String id,
            @Valid @RequestBody UpdateContractTemplateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractTemplateDto template = templateService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(template, MessageConstant.CONTRACT_TEMPLATE_UPDATED_SUCCESS));
    }

    @DeleteMapping("/contract-templates/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteContractTemplate(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        templateService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.CONTRACT_TEMPLATE_DELETED_SUCCESS));
    }

    @PutMapping("/contract-templates/{id}/set-default")
    public ResponseEntity<ApiResponse<ContractTemplateDto>> setDefaultContractTemplate(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ContractTemplateDto template = templateService.setDefault(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(template, MessageConstant.CONTRACT_TEMPLATE_SET_DEFAULT_SUCCESS));
    }

    @PostMapping("/contract-templates/{id}/preview")
    public ResponseEntity<ApiResponse<String>> previewContractTemplate(
            @PathVariable String id,
            @RequestBody Map<String, String> data,
            @AuthenticationPrincipal UserPrincipal principal) {
        String html = templateService.preview(id, data, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(html));
    }

    // ==================== INVOICES ====================

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getAllInvoices(
            @RequestParam(required = false) String houseId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String year,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<InvoiceDto> invoices = invoiceService.getAllByLandlord(principal.getId(), houseId, status, month, year);
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/invoices/summary")
    public ResponseEntity<ApiResponse<InvoiceSummaryDto>> getInvoiceSummary(
            @RequestParam(required = false) String houseId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceSummaryDto summary = invoiceService.getSummary(principal.getId(), houseId, month, year);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    @GetMapping("/invoices/reconciliation")
    public ResponseEntity<ApiResponse<UtilityReconciliationDto>> getUtilityReconciliation(
            @RequestParam(required = false) String houseId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        UtilityReconciliationDto reconciliation = invoiceService.getUtilityReconciliation(
                principal.getId(), houseId, month, year);
        return ResponseEntity.ok(ApiResponse.success(reconciliation));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getInvoiceById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceDto invoice = invoiceService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @PostMapping("/invoices/generate")
    public ResponseEntity<ApiResponse<InvoiceDto>> generateInvoice(
            @Valid @RequestBody CreateInvoiceRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceDto invoice = invoiceService.generate(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoice, MessageConstant.INVOICE_GENERATED_SUCCESS));
    }

    @PutMapping("/invoices/{id}/send")
    public ResponseEntity<ApiResponse<InvoiceDto>> sendInvoice(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceDto invoice = invoiceService.send(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoice, MessageConstant.INVOICE_SENT_SUCCESS));
    }

    @PostMapping("/invoices/{id}/send-sms")
    public ResponseEntity<ApiResponse<Void>> sendInvoiceSms(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        invoiceService.sendSms(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.SMS_SENT_SUCCESS));
    }

    @PutMapping("/invoices/{id}/cancel")
    public ResponseEntity<ApiResponse<InvoiceDto>> cancelInvoice(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceDto invoice = invoiceService.cancel(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoice, MessageConstant.INVOICE_CANCELLED_SUCCESS));
    }

    @DeleteMapping("/invoices/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteInvoice(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        invoiceService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.INVOICE_DELETED_SUCCESS));
    }

    @PostMapping("/invoices/adjustment")
    public ResponseEntity<ApiResponse<InvoiceDto>> createAdjustmentInvoice(
            @Valid @RequestBody CreateAdjustmentDto request,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceDto invoice = invoiceService.createAdjustmentInvoice(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(invoice, "Hóa đơn điều chỉnh đã được tạo"));
    }

    @GetMapping("/invoices/existing-contracts")
    public ResponseEntity<ApiResponse<List<String>>> getContractsWithInvoice(@RequestParam String periodMonth) {
        List<String> contractIds = invoiceService.getContractIdsWithInvoiceForPeriod(periodMonth);
        return ResponseEntity.ok(ApiResponse.success(contractIds));
    }

    @PostMapping("/invoices/{id}/payments")
    public ResponseEntity<ApiResponse<PaymentDto>> recordInvoicePayment(
            @PathVariable String id,
            @Valid @RequestBody CashPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PaymentDto payment = invoiceService.recordManualPayment(
                id,
                request.amount(),
                request.method() != null ? request.method() : "CASH",
                request.note(),
                principal.getId());
        return ResponseEntity.ok(ApiResponse.success(payment, MessageConstant.PAYMENT_RECORDED_SUCCESS));
    }

    @GetMapping("/invoices/status-summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getInvoiceStatusSummary(
            @RequestParam String houseId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        var summary = invoiceService.getInvoiceStatusForPeriod(principal.getId(), houseId, month, year);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ==================== METER READINGS ====================

    @PostMapping("/meter-readings")
    public ResponseEntity<ApiResponse<Void>> saveMeterReading(
            @Valid @RequestBody MeterReadingDto reading,
            @AuthenticationPrincipal UserPrincipal principal) {
        invoiceService.saveMeterReading(reading, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.METER_READING_SAVED_SUCCESS));
    }

    @GetMapping("/rooms/{roomId}/meter-readings")
    public ResponseEntity<ApiResponse<List<MeterReadingDto>>> getMeterReadings(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<MeterReadingDto> readings = invoiceService.getMeterReadings(roomId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(readings));
    }

    @GetMapping("/rooms/{roomId}/meter-readings/latest")
    public ResponseEntity<ApiResponse<MeterReadingDto>> getLatestMeterReading(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal principal) {
        MeterReadingDto reading = invoiceService.getLatestMeterReading(roomId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(reading));
    }

    @GetMapping("/rooms/{roomId}/meter-readings/{periodMonth}")
    public ResponseEntity<ApiResponse<MeterReadingDto>> getMeterReadingByPeriod(
            @PathVariable String roomId,
            @PathVariable String periodMonth,
            @AuthenticationPrincipal UserPrincipal principal) {
        MeterReadingDto reading = invoiceService.getMeterReadingByPeriod(roomId, periodMonth, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(reading));
    }

    @GetMapping("/meter-readings/batch")
    public ResponseEntity<ApiResponse<Map<String, MeterReadingDto>>> getMeterReadingsBatch(
            @RequestParam String periodMonth,
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, MeterReadingDto> readings = invoiceService.getMeterReadingsBatch(periodMonth, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(readings));
    }

    // ==================== UTILITY SHORTFALLS ====================

    @PostMapping("/shortfalls/flag")
    public ResponseEntity<ApiResponse<UtilityShortfallDto>> flagShortfall(
            @RequestParam String houseId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        var shortfall = invoiceService.flagShortfall(principal.getId(), houseId, month, year);
        return ResponseEntity.ok(ApiResponse.success(
                UtilityShortfallDto.fromEntity(shortfall),
                MessageConstant.SHORTFALL_FLAGGED_SUCCESS));
    }

    @GetMapping("/shortfalls/pending")
    public ResponseEntity<ApiResponse<List<UtilityShortfallDto>>> getPendingShortfalls(
            @RequestParam String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        var shortfalls = invoiceService.getPendingShortfalls(principal.getId(), houseId);
        return ResponseEntity.ok(ApiResponse.success(
                shortfalls.stream().map(UtilityShortfallDto::fromEntity).toList()));
    }

    @PutMapping("/shortfalls/{id}/apply")
    public ResponseEntity<ApiResponse<UtilityShortfallDto>> markShortfallApplied(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        var shortfall = invoiceService.markShortfallApplied(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(
                UtilityShortfallDto.fromEntity(shortfall),
                MessageConstant.SHORTFALL_MARKED_APPLIED_SUCCESS));
    }

    @DeleteMapping("/shortfalls/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteShortfall(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        invoiceService.deleteShortfall(principal.getId(), id);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.SHORTFALL_REMOVED_SUCCESS));
    }

    @PostMapping("/shortfalls/apply")
    public ResponseEntity<ApiResponse<Integer>> applyShortfall(
            @RequestParam String houseId,
            @RequestParam Integer month,
            @RequestParam Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        int count = invoiceService.applyShortfallToInvoices(principal.getId(), houseId, month, year);
        String message = count > 0
                ? "Đã áp dụng bù điện nước cho " + count + " hóa đơn"
                : "Không có hóa đơn nào được cập nhật";
        return ResponseEntity.ok(ApiResponse.success(count, message));
    }

    // ==================== PAYMENTS ====================

    @GetMapping("/invoices/{invoiceId}/payments")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getPaymentsByInvoice(
            @PathVariable String invoiceId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<PaymentDto> payments = paymentService.getByInvoice(invoiceId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/payments/cash")
    public ResponseEntity<ApiResponse<PaymentDto>> recordCashPayment(
            @Valid @RequestBody CashPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        PaymentDto payment = paymentService.recordCash(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(payment, MessageConstant.CASH_PAYMENT_RECORDED_SUCCESS));
    }

    @PostMapping("/payments/{paymentId}/confirm")
    public ResponseEntity<ApiResponse<PaymentDto>> confirmQRPayment(
            @PathVariable String paymentId,
            @AuthenticationPrincipal UserPrincipal principal) {
        PaymentDto payment = paymentService.confirmQR(paymentId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(payment, MessageConstant.PAYMENT_CONFIRMED_SUCCESS));
    }

    // ==================== SERVICE FEES ====================

    @GetMapping("/houses/{houseId}/service-fees")
    public ResponseEntity<ApiResponse<List<ServiceFeeDto>>> getServiceFees(
            @PathVariable String houseId,
            @RequestParam(required = false, defaultValue = "false") boolean activeOnly,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ServiceFeeDto> fees = activeOnly
                ? serviceFeeService.getActiveByHouse(houseId, principal.getId())
                : serviceFeeService.getByHouse(houseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(fees));
    }

    @GetMapping("/houses/{houseId}/service-fees/{id}")
    public ResponseEntity<ApiResponse<ServiceFeeDto>> getServiceFeeById(
            @PathVariable String houseId,
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceFeeDto fee = serviceFeeService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(fee));
    }

    @PostMapping("/houses/{houseId}/service-fees")
    public ResponseEntity<ApiResponse<ServiceFeeDto>> createServiceFee(
            @PathVariable String houseId,
            @Valid @RequestBody CreateServiceFeeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceFeeDto fee = serviceFeeService.create(houseId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(fee, MessageConstant.SERVICE_FEE_CREATED_SUCCESS));
    }

    @PutMapping("/houses/{houseId}/service-fees/{id}")
    public ResponseEntity<ApiResponse<ServiceFeeDto>> updateServiceFee(
            @PathVariable String houseId,
            @PathVariable String id,
            @Valid @RequestBody UpdateServiceFeeRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ServiceFeeDto fee = serviceFeeService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(fee, MessageConstant.SERVICE_FEE_UPDATED_SUCCESS));
    }

    @DeleteMapping("/houses/{houseId}/service-fees/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteServiceFee(
            @PathVariable String houseId,
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        serviceFeeService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.SERVICE_FEE_DELETED_SUCCESS));
    }

    // ==================== AMENITIES ====================

    @GetMapping("/amenities")
    public ResponseEntity<ApiResponse<List<AmenityDto>>> getAllAmenities(
            @RequestParam String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<AmenityDto> amenities = amenityService.getAll(houseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenities));
    }

    @PostMapping("/amenities")
    public ResponseEntity<ApiResponse<AmenityDto>> createAmenity(
            @Valid @RequestBody CreateAmenityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AmenityDto amenity = amenityService.create(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenity, MessageConstant.AMENITY_CREATED_SUCCESS));
    }

    @PutMapping("/amenities/{id}")
    public ResponseEntity<ApiResponse<AmenityDto>> updateAmenity(
            @PathVariable String id,
            @Valid @RequestBody UpdateAmenityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        AmenityDto amenity = amenityService.update(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenity, MessageConstant.AMENITY_UPDATED_SUCCESS));
    }

    @DeleteMapping("/amenities/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteAmenity(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        amenityService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.AMENITY_DELETED_SUCCESS));
    }

    // ==================== ROOM AMENITIES ====================

    @GetMapping("/rooms/{roomId}/amenities")
    public ResponseEntity<ApiResponse<List<RoomAmenityDto>>> getRoomAmenities(
            @PathVariable String roomId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<RoomAmenityDto> amenities = amenityService.getRoomAmenities(roomId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenities));
    }

    @PostMapping("/rooms/{roomId}/amenities")
    public ResponseEntity<ApiResponse<RoomAmenityDto>> addRoomAmenity(
            @PathVariable String roomId,
            @Valid @RequestBody AddRoomAmenityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoomAmenityDto amenity = amenityService.addRoomAmenity(roomId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenity, MessageConstant.AMENITY_ADDED_TO_ROOM_SUCCESS));
    }

    @PutMapping("/rooms/{roomId}/amenities/{amenityId}")
    public ResponseEntity<ApiResponse<RoomAmenityDto>> updateRoomAmenity(
            @PathVariable String roomId,
            @PathVariable String amenityId,
            @Valid @RequestBody UpdateRoomAmenityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        RoomAmenityDto amenity = amenityService.updateRoomAmenity(roomId, amenityId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenity, MessageConstant.ROOM_AMENITY_UPDATED_SUCCESS));
    }

    @DeleteMapping("/rooms/{roomId}/amenities/{amenityId}")
    public ResponseEntity<ApiResponse<Void>> removeRoomAmenity(
            @PathVariable String roomId,
            @PathVariable String amenityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        amenityService.removeRoomAmenity(roomId, amenityId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.AMENITY_REMOVED_FROM_ROOM_SUCCESS));
    }

    // ==================== HOUSE SHARED AMENITIES ====================

    @GetMapping("/houses/{houseId}/shared-amenities")
    public ResponseEntity<ApiResponse<List<HouseSharedAmenityDto>>> getHouseSharedAmenities(
            @PathVariable String houseId,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<HouseSharedAmenityDto> amenities = amenityService.getHouseSharedAmenities(houseId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenities));
    }

    @PostMapping("/houses/{houseId}/shared-amenities")
    public ResponseEntity<ApiResponse<HouseSharedAmenityDto>> addHouseSharedAmenity(
            @PathVariable String houseId,
            @RequestBody @Valid AddHouseSharedAmenityRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        HouseSharedAmenityDto amenity = amenityService.addHouseSharedAmenity(
                houseId, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenity, MessageConstant.SHARED_AMENITY_ADDED_SUCCESS));
    }

    @DeleteMapping("/houses/{houseId}/shared-amenities/{amenityId}")
    public ResponseEntity<ApiResponse<Void>> removeHouseSharedAmenity(
            @PathVariable String houseId,
            @PathVariable String amenityId,
            @AuthenticationPrincipal UserPrincipal principal) {
        amenityService.removeHouseSharedAmenity(houseId, amenityId, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.SHARED_AMENITY_REMOVED_SUCCESS));
    }

    @PutMapping("/houses/{houseId}/shared-amenities")
    public ResponseEntity<ApiResponse<List<HouseSharedAmenityDto>>> setHouseSharedAmenities(
            @PathVariable String houseId,
            @RequestBody Map<String, List<String>> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<HouseSharedAmenityDto> amenities = amenityService.setHouseSharedAmenities(
                houseId, request.get("amenityIds"), principal.getId());
        return ResponseEntity.ok(ApiResponse.success(amenities, MessageConstant.SHARED_AMENITIES_UPDATED_SUCCESS));
    }

    // ==================== EXPENSES ====================

    @GetMapping("/expenses")
    public ResponseEntity<ApiResponse<List<ExpenseDto>>> getAllExpenses(
            @RequestParam(required = false) String houseId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String year,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ExpenseDto> expenses = expenseService.getAllByLandlord(
                principal.getId(), houseId, categoryId, status, month, year);
        return ResponseEntity.ok(ApiResponse.success(expenses));
    }

    @GetMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<ExpenseDto>> getExpenseById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExpenseDto expense = expenseService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(expense));
    }

    @PostMapping("/expenses")
    public ResponseEntity<ApiResponse<ExpenseDto>> createExpense(
            @Valid @RequestBody CreateExpenseRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExpenseDto expense = expenseService.create(request, principal.getId(), null);
        return ResponseEntity.ok(ApiResponse.success(expense, MessageConstant.EXPENSE_CREATED_SUCCESS));
    }

    @PutMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<ExpenseDto>> updateExpense(
            @PathVariable String id,
            @Valid @RequestPart("data") UpdateExpenseRequest request,
            @RequestPart(value = "receipt", required = false) MultipartFile receipt,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExpenseDto expense = expenseService.update(id, request, principal.getId(), receipt);
        return ResponseEntity.ok(ApiResponse.success(expense, MessageConstant.EXPENSE_UPDATED_SUCCESS));
    }

    @DeleteMapping("/expenses/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteExpense(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        expenseService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.EXPENSE_DELETED_SUCCESS));
    }

    @PutMapping("/expenses/{id}/pay")
    public ResponseEntity<ApiResponse<ExpenseDto>> markExpenseAsPaid(
            @PathVariable String id,
            @Valid @RequestBody MarkExpensePaidRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExpenseDto expense = expenseService.markAsPaid(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(expense, MessageConstant.EXPENSE_MARKED_PAID_SUCCESS));
    }

    @GetMapping("/expenses/summary")
    public ResponseEntity<ApiResponse<ExpenseSummaryDto>> getExpenseSummary(
            @RequestParam(required = false) String houseId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @AuthenticationPrincipal UserPrincipal principal) {
        ExpenseSummaryDto summary = expenseService.getSummary(principal.getId(), houseId, month, year);
        return ResponseEntity.ok(ApiResponse.success(summary));
    }

    // ==================== TICKETS ====================

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getAllTickets(
            @RequestParam(required = false) String houseId,
            @RequestParam(required = false) String status,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TicketDto> tickets = ticketService.getAllByLandlord(principal.getId(), houseId, status);
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @GetMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<TicketDto>> getTicketById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.getById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(ticket));
    }

    @PutMapping("/tickets/{id}")
    public ResponseEntity<ApiResponse<TicketDto>> updateTicketStatus(
            @PathVariable String id,
            @Valid @RequestBody UpdateTicketRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.updateStatus(id, request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(ticket, MessageConstant.TICKET_UPDATED_SUCCESS));
    }

    // ==================== OCR ====================

    @PostMapping(value = "/ocr/scan-and-structure", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ScanContractResponse>> scanAndStructureContract(
            @RequestParam("images") List<MultipartFile> images) throws IOException {
        log.info("Received {} images for contract scanning", images.size());

        if (images.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(MessageConstant.UPLOAD_AT_LEAST_ONE_IMAGE));
        }

        if (images.size() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(MessageConstant.MAX_10_IMAGES));
        }

        List<MultipartFile> validImages = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image.isEmpty())
                continue;

            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(MessageConstant.FILE_NOT_IMAGE + image.getOriginalFilename()));
            }

            if (image.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(MessageConstant.FILE_TOO_LARGE + image.getOriginalFilename()));
            }

            validImages.add(image);
        }

        if (validImages.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(MessageConstant.NO_VALID_IMAGES));
        }

        ScanContractResponse result = ocrService.scanAndStructure(validImages);
        return ResponseEntity.ok(ApiResponse.success(result, MessageConstant.CONTRACT_EXTRACTED_SUCCESS));
    }

    @PostMapping(value = "/ocr/extract-contract-data", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<com.pegacorn.rently.dto.ocr.ExtractContractDataResponse>> extractContractData(
            @RequestParam("images") List<MultipartFile> images) throws IOException {
        log.info("Received {} images for contract data extraction", images.size());

        if (images.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(MessageConstant.UPLOAD_AT_LEAST_ONE_IMAGE));
        }

        if (images.size() > 10) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(MessageConstant.MAX_10_IMAGES));
        }

        List<MultipartFile> validImages = new ArrayList<>();
        for (MultipartFile image : images) {
            if (image.isEmpty())
                continue;

            String contentType = image.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(MessageConstant.FILE_NOT_IMAGE + image.getOriginalFilename()));
            }

            if (image.getSize() > 10 * 1024 * 1024) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error(MessageConstant.FILE_TOO_LARGE + image.getOriginalFilename()));
            }

            validImages.add(image);
        }

        if (validImages.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(MessageConstant.NO_VALID_IMAGES));
        }

        var result = ocrService.extractContractData(validImages);
        return ResponseEntity.ok(ApiResponse.success(result, MessageConstant.CONTRACT_DATA_EXTRACTED_SUCCESS));
    }

    // ==================== TASKS ====================

    @GetMapping("/tasks")
    public ResponseEntity<ApiResponse<List<TaskDto>>> getAllTasks(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TaskDto> tasks = taskService.getAll(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(tasks));
    }

    @PostMapping("/tasks")
    public ResponseEntity<ApiResponse<TaskDto>> createTask(
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskDto task = taskService.create(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(task, MessageConstant.TASK_CREATED_SUCCESS));
    }

    @PutMapping("/tasks/{id}/toggle-done")
    public ResponseEntity<ApiResponse<TaskDto>> toggleTaskDone(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskDto task = taskService.toggleDone(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @PutMapping("/tasks/{id}/toggle-pin")
    public ResponseEntity<ApiResponse<TaskDto>> toggleTaskPin(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        TaskDto task = taskService.togglePin(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(task));
    }

    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteTask(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        taskService.delete(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.TASK_DELETED_SUCCESS));
    }

    // ==================== TENANT LOOKUP (SECURE) ====================

    @PostMapping("/users/check-existence")
    public ResponseEntity<ApiResponse<Boolean>> checkUserExistence(
            @RequestBody Map<String, String> request,
            @AuthenticationPrincipal UserPrincipal principal) {
        String phone = request.get("phone");
        boolean userExists = authService.checkUserExistence(phone);
        if (!userExists) {
            return ResponseEntity.ok(ApiResponse.success(false));
        }

        // If user exists, check if they are already renting from this landlord
        boolean isCurrentTenant = tenantService.isTenantOfLandlord(phone, principal.getId());

        // Only require verification if they exist BUT are NOT already renting from this
        // landlord
        return ResponseEntity.ok(ApiResponse.success(!isCurrentTenant));
    }

    @PostMapping("/users/otp/request")
    public ResponseEntity<ApiResponse<Void>> requestTenantOtp(@RequestBody OtpRequest request) {
        // Force type to TENANT_VERIFICATION to prevent misuse
        OtpRequest secureRequest = new OtpRequest(
                request.phone(),
                OtpVerification.OtpType.TENANT_VERIFICATION);
        authService.requestOtp(secureRequest);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.OTP_SENT_SUCCESS));
    }

    @PostMapping("/users/otp/verify")
    public ResponseEntity<ApiResponse<UserDto>> verifyTenantOtp(@RequestBody VerifyOtpRequest request) {
        UserDto user = authService.verifyTenantAccessOtp(request);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
}
