package com.pegacorn.rently.dto.contract;

import com.pegacorn.rently.entity.Contract.DurationUnit;
import com.pegacorn.rently.entity.Contract.PaymentPeriod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.math.BigDecimal;
import java.util.List;

public record CreateContractRequest(
        @NotBlank(message = "Room ID is required")
        String roomId,

        // Primary tenant - either existing (by phone) or new (in tenants list with isPrimary=true)
        String primaryTenantPhone,

        // New tenants to create with the contract
        List<TenantRequest> tenants,

        // Duration
        @NotNull(message = "Duration is required")
        @Min(value = 1, message = "Duration must be at least 1")
        Integer duration,

        @NotNull(message = "Duration unit is required")
        DurationUnit durationUnit,

        @NotBlank(message = "Start date is required")
        String startDate,

        // Payment
        @NotNull(message = "Payment period is required")
        PaymentPeriod paymentPeriod,

        @NotNull(message = "Payment due day is required")
        @Min(value = 1, message = "Payment due day must be at least 1")
        Integer paymentDueDay,

        @NotNull(message = "Monthly rent is required")
        @DecimalMin(value = "0", message = "Monthly rent cannot be negative")
        BigDecimal monthlyRent,

        @NotNull(message = "Deposit months is required")
        @Min(value = 0, message = "Deposit months cannot be negative")
        Integer depositMonths,

        // Service fees
        List<ContractServiceFeeRequest> serviceFees,

        // Template
        String templateId,

        String customTerms,

        // Landlord info updates (optional)
        String landlordAddress,
        String landlordIdNumber,
        String landlordIdIssueDate,
        String landlordIdIssuePlace
) {
    public record ContractServiceFeeRequest(
            String serviceFeeId,
            BigDecimal amount
    ) {}

    public record TenantRequest(
            @NotBlank(message = "Phone is required")
            @Pattern(regexp = "^0[0-9]{9}$", message = "Invalid phone number format")
            String phone,

            @NotBlank(message = "Full name is required")
            String fullName,

            String idNumber,
            String idIssueDate,
            String idIssuePlace,
            String dateOfBirth,
            String placeOfOrigin,
            String gender,

            boolean isPrimary,
            boolean isExisting  // true if tenant already exists in DB
    ) {}
}
