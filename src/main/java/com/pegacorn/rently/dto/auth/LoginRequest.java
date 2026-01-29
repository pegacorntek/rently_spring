package com.pegacorn.rently.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record LoginRequest(
                @NotBlank(message = "Phone number is required") @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Invalid phone number format") String phone,

                @NotBlank(message = "Password is required") String password) {
}
