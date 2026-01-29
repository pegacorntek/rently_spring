package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "invoices")
public class Invoice {
    @Id
    private String id;

    @Column(name = "contract_id", nullable = false)
    private String contractId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "paid_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal paidAmount;

    @Column(name = "late_fee_percent", nullable = false, precision = 5, scale = 2)
    private BigDecimal lateFeePercent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private List<InvoiceItem> items;

    @Transient
    private TenantInfo tenant;

    @Transient
    private RoomInfo room;

    public enum InvoiceStatus {
        DRAFT, SENT, PARTIALLY_PAID, PAID, OVERDUE, CANCELLED
    }

    public enum InvoiceType {
        NORMAL, // Hóa đơn chính hàng tháng
        ADJUSTMENT, // Hóa đơn điều chỉnh chênh lệch
        CUSTOM_EXPENSE // Chi đột xuất
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "invoice_type", nullable = false)
    @Builder.Default
    private InvoiceType invoiceType = InvoiceType.NORMAL;

    @Column(name = "is_netting")
    @Builder.Default
    private Boolean isNetting = false;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantInfo {
        private String fullName;
        private String phone;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        private String code;
        private String houseName;
    }
}
