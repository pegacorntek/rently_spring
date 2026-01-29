package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payments")
public class Payment {
    @Id
    private String id;

    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentMethod method;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentStatus status;

    @Column(name = "transaction_code", length = 100)
    private String transactionCode;

    @Column(name = "proof_image_url", length = 500)
    private String proofImageUrl;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "sepay_transaction_id", length = 50)
    private String sepayTransactionId;

    public enum PaymentMethod {
        SEPAY, BANK_QR, CASH
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}
