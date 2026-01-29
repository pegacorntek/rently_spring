package com.pegacorn.rently.dto.auth;

import jakarta.validation.constraints.Size;

public record UpdateBankSettingsRequest(
        @Size(max = 50, message = "Bank name must be less than 50 characters")
        String bankName,

        @Size(max = 20, message = "Bank code must be less than 20 characters")
        String bankCode,

        @Size(max = 30, message = "Account number must be less than 30 characters")
        String bankAccountNumber,

        @Size(max = 100, message = "Account holder must be less than 100 characters")
        String bankAccountHolder
) {}
