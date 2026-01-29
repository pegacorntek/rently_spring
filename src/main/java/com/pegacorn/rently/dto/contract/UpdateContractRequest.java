package com.pegacorn.rently.dto.contract;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

public record UpdateContractRequest(
        String endDate,

        @DecimalMin(value = "0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,

        // Template fields (only editable for DRAFT contracts)
        String templateId,
        String customTerms
) {}
