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
@Table(name = "invoice_items")
public class InvoiceItem {
    @Id
    private String id;

    @Column(name = "invoice_id", nullable = false)
    private String invoiceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private InvoiceItemType type;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum InvoiceItemType {
        ROOM_RENT, ELECTRICITY, WATER, SERVICE, OTHER
    }
}
