package com.pegacorn.rently.dto.payment;

public record InitPaymentResponse(
        String paymentId,
        String paymentUrl,
        String qrCode
) {}
