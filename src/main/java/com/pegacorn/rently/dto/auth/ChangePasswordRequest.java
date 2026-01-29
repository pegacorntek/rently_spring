package com.pegacorn.rently.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank(message = "Current password is required") String currentPassword,

        @NotBlank(message = "New password is required") @Size(min = 6, max = 100, message = "Password must be 6-100 characters") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain both letters and numbers") String newPassword,

        @NotBlank(message = "Retype password is required") String retypePassword) {
}
