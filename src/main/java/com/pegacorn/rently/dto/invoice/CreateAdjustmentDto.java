package com.pegacorn.rently.dto.invoice;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for creating ADJUSTMENT invoices from reconciliation diffs.
 */
public record CreateAdjustmentDto(
        String houseId,
        String periodMonth, // YYYY-MM
        AdjustmentMode mode,
        List<DiffItem> diffs,
        Integer dueDays // Default 5 days
) {
    public enum AdjustmentMode {
        POSITIVE_ONLY, // Chỉ thu thêm (diff > 0)
        NEGATIVE_ONLY, // Chỉ hoàn tiền (diff < 0)
        NET // Gộp & bù trừ
    }

    public record DiffItem(
            String serviceName,
            BigDecimal amount // Can be positive or negative
    ) {
    }
}
