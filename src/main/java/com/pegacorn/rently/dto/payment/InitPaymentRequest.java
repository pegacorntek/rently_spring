package com.pegacorn.rently.dto.payment;

import com.pegacorn.rently.entity.Payment;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitPaymentRequest(
        @NotBlank(message = "Invoice ID is required")
        String invoiceId,

        @NotNull(message = "Payment method is required")
        Payment.PaymentMethod method,

        @NotNull(message = "Amount is required")
        @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
        BigDecimal amount
) {}
