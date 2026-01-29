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
@Table(name = "sepay_transactions")
public class SepayTransaction {
    @Id
    private String id;

    @Column(name = "sepay_transaction_id", nullable = false, unique = true, length = 50)
    private String sepayTransactionId;

    @Column(nullable = false, length = 20)
    private String gateway;

    @Column(name = "account_number", nullable = false, length = 30)
    private String accountNumber;

    @Column(name = "transfer_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal transferAmount;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String code;

    @Column(name = "reference_code", length = 100)
    private String referenceCode;

    @Column(name = "transaction_date", nullable = false)
    private LocalDateTime transactionDate;

    @Column(name = "transfer_type", nullable = false, length = 10)
    private String transferType;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "invoice_id", length = 36)
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionStatus status;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum TransactionStatus {
        RECEIVED,   // Transaction received from SePay
        MATCHED,    // Successfully matched to an invoice
        UNMATCHED,  // Could not match to any invoice
        IGNORED     // Ignored (e.g., outgoing transfer)
    }
}
