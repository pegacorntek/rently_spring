package com.pegacorn.rently.dto.expense;

import java.math.BigDecimal;

public record ExpenseSummaryDto(
        BigDecimal totalAmount,
        BigDecimal paidAmount,
        BigDecimal pendingAmount,
        int totalCount,
        int paidCount,
        int pendingCount
) {}
