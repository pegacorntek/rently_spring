package com.pegacorn.rently.dto.tenant;

import jakarta.validation.constraints.NotNull;

public record UpdatePrivacyRequest(
                @NotNull Boolean shareFullName,
                @NotNull Boolean sharePhone,
                @NotNull Boolean shareOrigin,
                @NotNull Boolean shareGender) {
}
