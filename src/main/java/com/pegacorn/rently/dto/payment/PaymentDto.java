package com.pegacorn.rently.dto.payment;

import com.pegacorn.rently.entity.Payment;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PaymentDto(
        String id,
        String invoiceId,
        BigDecimal amount,
        String method,
        String status,
        String transactionCode,
        String proofImageUrl,
        LocalDateTime paidAt,
        LocalDateTime createdAt,
        // Room/House info
        String roomCode,
        String houseName,
        String periodMonth
) {
    public static PaymentDto fromEntity(Payment payment) {
        return new PaymentDto(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getTransactionCode(),
                payment.getProofImageUrl(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                null,
                null,
                null
        );
    }

    public static PaymentDto fromEntity(Payment payment, String roomCode, String houseName, String periodMonth) {
        return new PaymentDto(
                payment.getId(),
                payment.getInvoiceId(),
                payment.getAmount(),
                payment.getMethod().name(),
                payment.getStatus().name(),
                payment.getTransactionCode(),
                payment.getProofImageUrl(),
                payment.getPaidAt(),
                payment.getCreatedAt(),
                roomCode,
                houseName,
                periodMonth
        );
    }
}
