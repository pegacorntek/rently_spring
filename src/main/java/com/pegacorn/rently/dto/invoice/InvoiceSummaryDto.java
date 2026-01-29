package com.pegacorn.rently.dto.invoice;

import java.math.BigDecimal;

public record InvoiceSummaryDto(
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        int totalCount,
        int paidCount,
        int unpaidCount,
        // Deposit data
        BigDecimal depositCollected,
        BigDecimal depositPending,
        int depositPaidCount,
        int depositPendingCount
) {}
