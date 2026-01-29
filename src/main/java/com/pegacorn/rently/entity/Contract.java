package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "landlord_id", nullable = false)
    private String landlordId;

    @Column(name = "tenant_id", nullable = false)
    private String tenantId;

    // Duration
    @Column(nullable = false)
    private Integer duration;

    @Enumerated(EnumType.STRING)
    @Column(name = "duration_unit", nullable = false)
    private DurationUnit durationUnit;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    // Payment
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_period", nullable = false)
    private PaymentPeriod paymentPeriod;

    @Column(name = "payment_due_day", nullable = false)
    private Integer paymentDueDay;

    @Column(name = "monthly_rent", nullable = false, precision = 15, scale = 2)
    private BigDecimal monthlyRent;

    @Column(name = "deposit_months", nullable = false)
    private Integer depositMonths;

    @Column(name = "deposit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal depositAmount;

    @Column(name = "deposit_paid", nullable = false)
    private boolean depositPaid;

    // Template
    @Column(name = "template_id")
    private String templateId;

    @Column(name = "custom_terms", columnDefinition = "TEXT")
    private String customTerms;

    // Snapshot of rendered content (frozen when contract is activated)
    @Column(name = "content_snapshot", columnDefinition = "TEXT")
    private String contentSnapshot;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ContractStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Transient
    private RoomInfo room;

    @Transient
    private TenantInfo tenant;

    @Transient
    private java.util.List<ContractServiceFee> serviceFees;

    public enum ContractStatus {
        DRAFT, ACTIVE, ENDED
    }

    public enum DurationUnit {
        MONTH, YEAR
    }

    public enum PaymentPeriod {
        MONTHLY, QUARTERLY, BIANNUAL, ANNUAL
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RoomInfo {
        private String id;
        private String code;
        private String houseName;
        private String houseId;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TenantInfo {
        private String id;
        private String fullName;
        private String phone;
    }
}
