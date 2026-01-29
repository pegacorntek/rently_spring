package com.pegacorn.rently.dto.room;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AddTenantRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "^0[0-9]{9}$", message = "Invalid phone number format")
        String phone,

        // Optional fields for creating new user
        String fullName,
        String idNumber,
        String gender,
        String dateOfBirth,
        String placeOfOrigin,
        String placeOfResidence,
        String idIssueDate,
        String idIssuePlace,

        // Set as primary tenant (representative)
        Boolean isPrimary
) {}
