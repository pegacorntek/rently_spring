package com.pegacorn.rently.dto.invoice;

import java.math.BigDecimal;
import java.util.List;

public record UtilityReconciliationDto(
        // Electricity (backward compatibility)
        BigDecimal electricityExpense,      // What landlord paid
        BigDecimal electricityCollected,    // What collected from tenants
        BigDecimal electricityShortfall,    // Difference
        BigDecimal electricityPerRoom,      // Suggested split per room

        // Water (backward compatibility)
        BigDecimal waterExpense,
        BigDecimal waterCollected,
        BigDecimal waterShortfall,
        BigDecimal waterPerRoom,

        // Totals
        BigDecimal totalShortfall,
        int activeRoomCount,

        // Detailed breakdown by category
        List<CategoryBreakdown> categories
) {
    /**
     * Breakdown for each expense category
     */
    public record CategoryBreakdown(
            String categoryId,
            String categoryName,
            String icon,
            BigDecimal expense,      // What landlord paid (from expenses)
            BigDecimal collected,    // What collected from tenants (from invoices)
            BigDecimal shortfall,    // expense - collected (if positive)
            BigDecimal perRoom       // shortfall / activeRoomCount
    ) {}
}
