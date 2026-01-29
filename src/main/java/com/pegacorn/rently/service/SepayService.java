package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.payment.SepayWebhookRequest;
import com.pegacorn.rently.dto.push.PushNotificationRequest;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SePay webhook processing service.
 * Receives bank transfer notifications and updates invoice status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SepayService {

    private final SepayTransactionRepository sepayTransactionRepository;
    private final InvoiceRepository invoiceRepository;
    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final PaymentRepository paymentRepository;
    private final PushNotificationService pushNotificationService;

    @Value("${sepay.api-key:}")
    private String sepayApiKey;

    // Pattern to extract invoice ID from payment description: INV{uuid}
    private static final Pattern INVOICE_PATTERN = Pattern.compile("INV([a-fA-F0-9-]{36})");

    /**
     * Process SePay webhook notification.
     * Called when a bank transfer is received.
     */
    @Transactional
    public void processWebhook(SepayWebhookRequest request, String apiKey) {
        log.info("Processing SePay webhook: id={}, amount={}, code={}, content={}",
                request.id(), request.transferAmount(), request.code(), request.content());

        // Validate API key if configured
        if (sepayApiKey != null && !sepayApiKey.isEmpty() && !sepayApiKey.equals(apiKey)) {
            log.warn("Invalid SePay API key for transaction: {}", request.id());
            throw ApiException.forbidden(MessageConstant.INVALID_API_KEY);
        }

        // Idempotency check
        if (sepayTransactionRepository.existsBySepayTransactionId(request.id())) {
            log.info("Duplicate transaction ignored: {}", request.id());
            return;
        }

        // Only process incoming transfers
        if (!"in".equalsIgnoreCase(request.transferType())) {
            log.info("Ignoring outgoing transfer: {}", request.id());
            saveTransaction(request, null, null, SepayTransaction.TransactionStatus.IGNORED);
            return;
        }

        // Extract invoice ID from code, content, or description
        String invoiceId = extractInvoiceId(request.code(), request.content(), request.description());

        if (invoiceId == null) {
            log.warn("Could not extract invoice ID: {} (code={}, content={})",
                    request.id(), request.code(), request.content());
            saveTransaction(request, null, null, SepayTransaction.TransactionStatus.UNMATCHED);
            return;
        }

        // Find invoice
        Invoice invoice = invoiceRepository.findById(invoiceId).orElse(null);
        if (invoice == null) {
            log.warn("Invoice not found: {} (invoiceId: {})", request.id(), invoiceId);
            saveTransaction(request, null, invoiceId, SepayTransaction.TransactionStatus.UNMATCHED);
            return;
        }

        // Check if payable
        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID ||
            invoice.getStatus() == Invoice.InvoiceStatus.CANCELLED) {
            log.info("Invoice {} already {} - ignoring", invoiceId, invoice.getStatus());
            saveTransaction(request, null, invoiceId, SepayTransaction.TransactionStatus.IGNORED);
            return;
        }

        // Check for existing PENDING payment - confirm it instead of creating duplicate
        List<Payment> pendingPayments = paymentRepository
                .findByInvoiceIdAndStatus(invoiceId, Payment.PaymentStatus.PENDING);

        Payment payment;
        if (!pendingPayments.isEmpty()) {
            // Confirm the existing pending payment
            payment = pendingPayments.get(0);
            payment.setStatus(Payment.PaymentStatus.SUCCESS);
            payment.setMethod(Payment.PaymentMethod.SEPAY);
            payment.setTransactionCode(request.id());
            payment.setNote("SePay: " + (request.content() != null ? request.content() : request.code()));
            payment.setPaidAt(parseDate(request.transactionDate()));
            payment.setSepayTransactionId(request.id());
        } else {
            // Create new payment
            payment = Payment.builder()
                    .id(UUID.randomUUID().toString())
                    .invoiceId(invoiceId)
                    .amount(request.transferAmount())
                    .method(Payment.PaymentMethod.SEPAY)
                    .status(Payment.PaymentStatus.SUCCESS)
                    .transactionCode(request.id())
                    .note("SePay: " + (request.content() != null ? request.content() : request.code()))
                    .paidAt(parseDate(request.transactionDate()))
                    .createdAt(LocalDateTime.now())
                    .sepayTransactionId(request.id())
                    .build();
        }

        paymentRepository.save(payment);

        // Update invoice
        updateInvoice(invoice);

        // Save transaction
        saveTransaction(request, payment.getId(), invoiceId, SepayTransaction.TransactionStatus.MATCHED);

        log.info("Payment processed: invoice={}, amount={}", invoiceId, request.transferAmount());

        // Send push notification
        sendNotification(invoice, request.transferAmount(), payment.getId());
    }

    private String extractInvoiceId(String code, String content, String description) {
        // Check all possible fields where the invoice ID might appear
        for (String field : new String[]{code, content, description}) {
            if (field != null) {
                Matcher m = INVOICE_PATTERN.matcher(field);
                if (m.find()) return m.group(1);
            }
        }
        return null;
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null) return LocalDateTime.now();
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }

    private void saveTransaction(SepayWebhookRequest req, String paymentId, String invoiceId,
                                  SepayTransaction.TransactionStatus status) {
        sepayTransactionRepository.save(SepayTransaction.builder()
                .id(UUID.randomUUID().toString())
                .sepayTransactionId(req.id())
                .gateway(req.gateway() != null ? req.gateway() : "unknown")
                .accountNumber(req.accountNumber() != null ? req.accountNumber() : "unknown")
                .transferAmount(req.transferAmount())
                .content(req.content())
                .code(req.code())
                .referenceCode(req.referenceCode())
                .transactionDate(parseDate(req.transactionDate()))
                .transferType(req.transferType() != null ? req.transferType() : "unknown")
                .paymentId(paymentId)
                .invoiceId(invoiceId)
                .status(status)
                .processedAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private void updateInvoice(Invoice invoice) {
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

    private void sendNotification(Invoice invoice, BigDecimal amount, String paymentId) {
        try {
            Contract contract = contractRepository.findById(invoice.getContractId()).orElse(null);
            if (contract == null) return;

            Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
            if (room == null) return;

            House house = houseRepository.findById(room.getHouseId()).orElse(null);
            if (house == null) return;

            NumberFormat fmt = NumberFormat.getInstance(Locale.of("vi", "VN"));

            pushNotificationService.sendToUser(house.getOwnerId(), new PushNotificationRequest(
                    "Thanh toán thành công",
                    String.format("Phòng %s đã thanh toán %sđ", room.getCode(), fmt.format(amount)),
                    null, null,
                    "payment-" + paymentId,
                    Map.of(
                            "type", "PAYMENT_RECEIVED",
                            "invoiceId", invoice.getId(),
                            "paymentId", paymentId,
                            "amount", amount,
                            "roomCode", room.getCode(),
                            "url", "/invoices"
                    ),
                    true
            ));
        } catch (Exception e) {
            log.error("Failed to send notification: {}", e.getMessage());
        }
    }
}
