package com.pegacorn.rently.dto.payment;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

/**
 * SePay IPN (Instant Payment Notification) webhook request payload.
 * See: https://developer.sepay.vn/en
 */
public record SepayWebhookRequest(
        @JsonProperty("id")
        String id,

        @JsonProperty("gateway")
        String gateway,

        @JsonProperty("transactionDate")
        String transactionDate,

        @JsonProperty("accountNumber")
        String accountNumber,

        @JsonProperty("code")
        String code,

        @JsonProperty("content")
        String content,

        @JsonProperty("transferType")
        String transferType,

        @JsonProperty("transferAmount")
        BigDecimal transferAmount,

        @JsonProperty("accumulated")
        BigDecimal accumulated,

        @JsonProperty("subAccount")
        String subAccount,

        @JsonProperty("referenceCode")
        String referenceCode,

        @JsonProperty("description")
        String description
) {}
