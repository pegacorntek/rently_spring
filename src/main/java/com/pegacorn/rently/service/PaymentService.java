package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.payment.*;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public List<PaymentDto> getByInvoice(String invoiceId, String landlordId) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

        Contract contract = contractRepository.findById(invoice.getContractId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return paymentRepository.findByInvoiceId(invoiceId).stream()
                .map(PaymentDto::fromEntity)
                .toList();
    }

    @Transactional
    public PaymentDto recordCash(CashPaymentRequest request, String landlordId) {
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

        Contract contract = contractRepository.findById(invoice.getContractId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Check for existing PENDING payment - confirm it instead of creating duplicate
        List<Payment> pendingPayments = paymentRepository
                .findByInvoiceIdAndStatus(request.invoiceId(), Payment.PaymentStatus.PENDING);

        Payment payment;
        if (!pendingPayments.isEmpty()) {
            // Confirm the existing pending payment
            payment = pendingPayments.get(0);
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setMethod(Payment.PaymentMethod.CASH);
            payment.setNote(request.note());
            payment.setPaidAt(LocalDateTime.now());
        } else {
            // Create new payment
            payment = Payment.builder()
                    .id(UUID.randomUUID().toString())
                    .invoiceId(request.invoiceId())
                    .amount(request.amount())
                    .method(Payment.PaymentMethod.CASH)
                    .status(Payment.PaymentStatus.SUCCESS)
                    .note(request.note())
                    .paidAt(LocalDateTime.now())
                    .createdAt(LocalDateTime.now())
                    .build();
        }

        paymentRepository.save(payment);
        updateInvoicePaidAmount(invoice);

        // Notify tenant that payment was confirmed
        notifyTenantPaymentConfirmed(invoice, payment);

        return PaymentDto.fromEntity(payment);
    }

    @Transactional
    public PaymentDto confirmQR(String paymentId, String landlordId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.PAYMENT_NOT_FOUND));

        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

        Contract contract = contractRepository.findById(invoice.getContractId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw ApiException.badRequest(MessageConstant.PAYMENT_NOT_PENDING);
        }

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setPaidAt(LocalDateTime.now());
        paymentRepository.save(payment);

        // Mark other duplicate PENDING payments for the same invoice as FAILED
        List<Payment> otherPendingPayments = paymentRepository
                .findByInvoiceIdAndStatus(invoice.getId(), Payment.PaymentStatus.PENDING);
        for (Payment otherPayment : otherPendingPayments) {
            if (!otherPayment.getId().equals(paymentId)) {
                otherPayment.setStatus(Payment.PaymentStatus.FAILED);
                otherPayment.setNote("Đã có thanh toán khác được xác nhận");
                paymentRepository.save(otherPayment);
            }
        }

        updateInvoicePaidAmount(invoice);

        // Notify tenant that payment was confirmed
        notifyTenantPaymentConfirmed(invoice, payment);

        return PaymentDto.fromEntity(payment);
    }

    private void notifyTenantPaymentConfirmed(Invoice invoice, Payment payment) {
        try {
            Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
            Room room = contract != null ? roomRepository.findById(contract.getRoomId()).orElse(null) : null;
            String roomCode = room != null ? room.getCode() : "";

            String title = "Thanh toán đã được xác nhận";
            String body = String.format("Chủ trọ đã xác nhận thanh toán %s cho hóa đơn %s - Phòng %s.",
                    formatCurrency(payment.getAmount()), invoice.getPeriodMonth(), roomCode);

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("invoiceId", invoice.getId());
            data.put("url", "/tenant/invoices");

            notificationService.createNotification(
                    invoice.getTenantId(),
                    Notification.NotificationType.PAYMENT_CONFIRMED,
                    title,
                    body,
                    data);
        } catch (Exception e) {
            // Log but don't fail the main operation
        }
    }

    private String formatCurrency(java.math.BigDecimal amount) {
        java.text.NumberFormat formatter = java.text.NumberFormat.getInstance(new java.util.Locale("vi", "VN"));
        return formatter.format(amount) + " ₫";
    }

    @Transactional
    public InitPaymentResponse initPayment(InitPaymentRequest request, String tenantId) {
        Invoice invoice = invoiceRepository.findById(request.invoiceId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

        if (!invoice.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID ||
            invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            throw ApiException.badRequest(MessageConstant.INVOICE_NOT_PAYABLE);
        }

        // Check for existing pending payment - return it instead of creating duplicate
        List<Payment> pendingPayments = paymentRepository
                .findByInvoiceIdAndStatus(request.invoiceId(), Payment.PaymentStatus.PENDING);

        Payment payment;
        if (!pendingPayments.isEmpty()) {
            // Return existing pending payment
            payment = pendingPayments.get(0);
        } else {
            // Create new payment
            payment = Payment.builder()
                    .id(UUID.randomUUID().toString())
                    .invoiceId(request.invoiceId())
                    .amount(request.amount())
                    .method(request.method())
                    .status(Payment.PaymentStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            paymentRepository.save(payment);
        }

        // TODO: Integrate with payment gateway (Sepay, etc.)
        String qrCode = null;
        String paymentUrl = null;

        if (request.method() == Payment.PaymentMethod.BANK_QR) {
            qrCode = "QR_CODE_DATA_" + payment.getId();
        } else if (request.method() == Payment.PaymentMethod.SEPAY) {
            paymentUrl = "https://sepay.vn/payment/" + payment.getId();
        }

        return new InitPaymentResponse(payment.getId(), paymentUrl, qrCode);
    }

    @Transactional
    public PaymentDto uploadProof(String paymentId, MultipartFile file, String tenantId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.PAYMENT_NOT_FOUND));

        Invoice invoice = invoiceRepository.findById(payment.getInvoiceId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.INVOICE_NOT_FOUND));

        if (!invoice.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (payment.getStatus() != Payment.PaymentStatus.PENDING) {
            throw ApiException.badRequest(MessageConstant.PAYMENT_NOT_PENDING);
        }

        String fileName = saveFile(file, "payments");
        payment.setProofImageUrl("/files/payments/" + fileName);
        paymentRepository.save(payment);

        // Notify landlord about payment proof upload
        notifyLandlordPaymentProof(invoice, tenantId);

        return PaymentDto.fromEntity(payment);
    }

    private void notifyLandlordPaymentProof(Invoice invoice, String tenantId) {
        try {
            Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
            if (contract == null) return;

            Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
            User tenant = userRepository.findById(tenantId).orElse(null);

            String tenantName = tenant != null ? tenant.getFullName() : "Người thuê";
            String roomCode = room != null ? room.getCode() : "";

            String title = "Xác nhận thanh toán - Phòng " + roomCode;
            String body = String.format("%s đã thanh toán hóa đơn %s. Vui lòng kiểm tra và xác nhận.",
                    tenantName, invoice.getPeriodMonth());

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("invoiceId", invoice.getId());
            data.put("url", "/landlord/billing?tab=history&invoiceId=" + invoice.getId());

            notificationService.createNotification(
                    contract.getLandlordId(),
                    Notification.NotificationType.PAYMENT_RECEIVED,
                    title,
                    body,
                    data);
        } catch (Exception e) {
            // Log but don't fail the main operation
        }
    }

    public List<PaymentDto> getMyPayments(String tenantId) {
        return paymentRepository.findByTenantId(tenantId).stream()
                .map(payment -> {
                    // Get invoice, contract, room, house to enrich payment
                    Invoice invoice = invoiceRepository.findById(payment.getInvoiceId()).orElse(null);
                    if (invoice == null) {
                        return PaymentDto.fromEntity(payment);
                    }

                    Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
                    if (contract == null) {
                        return PaymentDto.fromEntity(payment, null, null, invoice.getPeriodMonth());
                    }

                    Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
                    if (room == null) {
                        return PaymentDto.fromEntity(payment, null, null, invoice.getPeriodMonth());
                    }

                    House house = houseRepository.findById(room.getHouseId()).orElse(null);
                    String houseName = house != null ? house.getName() : null;

                    return PaymentDto.fromEntity(payment, room.getCode(), houseName, invoice.getPeriodMonth());
                })
                .toList();
    }

    private void updateInvoicePaidAmount(Invoice invoice) {
        List<Payment> successfulPayments = paymentRepository
                .findByInvoiceIdAndStatus(invoice.getId(), Payment.PaymentStatus.SUCCESS);

        BigDecimal totalPaid = successfulPayments.stream()
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

    private String saveFile(MultipartFile file, String folder) {
        try {
            Path uploadDir = Paths.get(uploadPath, folder);
            Files.createDirectories(uploadDir);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            return fileName;
        } catch (IOException e) {
            throw ApiException.badRequest(MessageConstant.FAILED_TO_SAVE_FILE + e.getMessage());
        }
    }
}
