package com.pegacorn.rently.dto.tenant;

import com.pegacorn.rently.entity.User;
import jakarta.validation.constraints.NotBlank;

public record UpdateTenantRequest(
        @NotBlank String fullName,
        String phone,
        String idNumber,
        String idIssueDate,
        String idIssuePlace,
        User.Gender gender,
        String dateOfBirth,
        String placeOfOrigin
) {}
