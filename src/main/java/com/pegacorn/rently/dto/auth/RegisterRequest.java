package com.pegacorn.rently.dto.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
                @NotBlank(message = "Phone number is required") @Pattern(regexp = "^(0|\\+84)[0-9]{9}$", message = "Invalid phone number format") String phone,

                @NotBlank(message = "OTP is required") @Size(min = 6, max = 6, message = "OTP must be 6 digits") String otp,

                @NotBlank(message = "Password is required") @Size(min = 6, max = 100, message = "Password must be 6-100 characters") @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Password must contain both letters and numbers") String password,

                @NotBlank(message = "Please confirm your password") String retypePassword,

                String fullName) {
}
