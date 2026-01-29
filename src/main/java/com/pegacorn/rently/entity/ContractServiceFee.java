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
@Table(name = "contract_service_fees")
public class ContractServiceFee {
    @Id
    private String id;

    @Column(name = "contract_id", nullable = false)
    private String contractId;

    @Column(name = "service_fee_id", nullable = false)
    private String serviceFeeId;

    // Snapshot of the fee name at the time of contract creation
    @Column(nullable = false)
    private String name;

    // Snapshot of the fee type at the time of contract creation
    @Column(name = "fee_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private ServiceFee.FeeType feeType;

    // Unit rate for this contract (per person, per unit, etc.)
    // For PER_PERSON fees, this is the rate per person
    // For FIXED fees, this equals the amount
    @Column(name = "unit_rate", precision = 12, scale = 2)
    private BigDecimal unitRate;

    // Total amount for this contract (calculated: unitRate × quantity for PER_PERSON, or just the fixed amount)
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    // Snapshot of the unit at the time of contract creation
    @Column
    private String unit;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
