package com.pegacorn.rently.dto.payment;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VietQRResponse {
    private String qrCodeUrl;
    private String bankName;
    private String bankCode;
    private String accountNumber;
    private String accountHolder;
    private BigDecimal amount;
    private String description;
    private String invoiceId;
}
