package com.pegacorn.rently.dto.auth;

import com.pegacorn.rently.entity.OtpVerification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpRequest(
                @NotBlank(message = "Phone number is required") @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Invalid phone number format") String phone,

                @NotNull(message = "OTP type is required") OtpVerification.OtpType type) {
}
