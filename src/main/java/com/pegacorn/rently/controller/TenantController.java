package com.pegacorn.rently.controller;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.common.ApiResponse;
import com.pegacorn.rently.dto.contract.ContractDto;
import com.pegacorn.rently.dto.contract.ContractSnapshotDto;
import com.pegacorn.rently.dto.invoice.InvoiceDto;
import com.pegacorn.rently.dto.payment.InitPaymentRequest;
import com.pegacorn.rently.dto.payment.InitPaymentResponse;
import com.pegacorn.rently.dto.payment.PaymentDto;
import com.pegacorn.rently.dto.payment.VietQRResponse;
import com.pegacorn.rently.dto.tenant.HousemateDto;
import com.pegacorn.rently.dto.ticket.TicketDto;
import com.pegacorn.rently.security.UserPrincipal;
import com.pegacorn.rently.service.ContractService;
import com.pegacorn.rently.service.InvoiceService;
import com.pegacorn.rently.service.PaymentService;
import com.pegacorn.rently.service.TenantService;
import com.pegacorn.rently.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tenant")
@RequiredArgsConstructor
public class TenantController {

    private final ContractService contractService;
    private final InvoiceService invoiceService;
    private final PaymentService paymentService;
    private final TicketService ticketService;
    private final TenantService tenantService;

    // ==================== CONTRACTS ====================

    @GetMapping("/contracts")
    public ResponseEntity<ApiResponse<List<ContractDto>>> getMyContracts(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ContractDto> contracts = contractService.getMyContracts(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(contracts));
    }

    @GetMapping("/contracts/{id}/render")
    public ResponseEntity<ApiResponse<String>> renderContractContent(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        String html = contractService.renderContentForTenant(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(html));
    }

    @GetMapping("/contracts/{id}/history")
    public ResponseEntity<ApiResponse<List<ContractSnapshotDto>>> getContractSnapshotHistory(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        List<ContractSnapshotDto> snapshots = contractService.getSnapshotHistoryForTenant(id, principal.getId())
                .stream()
                .map(ContractSnapshotDto::fromEntity)
                .toList();
        return ResponseEntity.ok(ApiResponse.success(snapshots));
    }

    // ==================== INVOICES ====================

    @GetMapping("/invoices")
    public ResponseEntity<ApiResponse<List<InvoiceDto>>> getMyInvoices(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<InvoiceDto> invoices = invoiceService.getMyInvoices(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoices));
    }

    @GetMapping("/invoices/{id}")
    public ResponseEntity<ApiResponse<InvoiceDto>> getMyInvoiceById(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        InvoiceDto invoice = invoiceService.getMyInvoiceById(id, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(invoice));
    }

    @GetMapping("/invoices/{id}/vietqr")
    public ResponseEntity<ApiResponse<VietQRResponse>> getInvoiceVietQR(
            @PathVariable String id,
            @AuthenticationPrincipal UserPrincipal principal) {
        // Verify tenant has access to this invoice
        invoiceService.getMyInvoiceById(id, principal.getId());
        // Generate QR code
        VietQRResponse qrData = invoiceService.generateVietQR(id);
        return ResponseEntity.ok(ApiResponse.success(qrData));
    }

    // ==================== PAYMENTS ====================

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<PaymentDto>>> getMyPayments(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<PaymentDto> payments = paymentService.getMyPayments(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(payments));
    }

    @PostMapping("/payments/init")
    public ResponseEntity<ApiResponse<InitPaymentResponse>> initPayment(
            @Valid @RequestBody InitPaymentRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        InitPaymentResponse response = paymentService.initPayment(request, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/payments/{paymentId}/upload-proof")
    public ResponseEntity<ApiResponse<PaymentDto>> uploadPaymentProof(
            @PathVariable String paymentId,
            @RequestParam("proof") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal) {
        PaymentDto payment = paymentService.uploadProof(paymentId, file, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(payment, MessageConstant.PROOF_UPLOADED_SUCCESS));
    }

    // ==================== TICKETS ====================

    @GetMapping("/tickets")
    public ResponseEntity<ApiResponse<List<TicketDto>>> getMyTickets(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<TicketDto> tickets = ticketService.getMyTickets(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(tickets));
    }

    @PostMapping("/tickets")
    public ResponseEntity<ApiResponse<TicketDto>> createTicket(
            @RequestParam("roomId") String roomId,
            @RequestParam("title") String title,
            @RequestParam("description") String description,
            @RequestParam(value = "attachments", required = false) List<MultipartFile> attachments,
            @AuthenticationPrincipal UserPrincipal principal) {
        TicketDto ticket = ticketService.create(roomId, title, description, attachments, principal.getId());
        return ResponseEntity.ok(ApiResponse.success(ticket, MessageConstant.TICKET_CREATED_SUCCESS));
    }

    // ==================== HOUSEMATES ====================

    @GetMapping("/housemates")
    public ResponseEntity<ApiResponse<List<HousemateDto>>> getMyHousemates(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<HousemateDto> housemates = tenantService.getMyHousemates(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(housemates));
    }

    @PutMapping("/privacy")
    public ResponseEntity<ApiResponse<Void>> updatePrivacySettings(
            @Valid @RequestBody com.pegacorn.rently.dto.tenant.UpdatePrivacyRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        tenantService.updatePrivacySettings(principal.getId(), request);
        return ResponseEntity.ok(ApiResponse.success(null, MessageConstant.PRIVACY_UPDATED_SUCCESS));
    }
}
