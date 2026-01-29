package com.pegacorn.rently.dto.contract;

import com.pegacorn.rently.entity.ContractServiceFee;
import java.math.BigDecimal;

public record ContractServiceFeeDto(
    String id,
    String serviceFeeId,
    String name,
    String feeType,
    BigDecimal unitRate,
    BigDecimal amount,
    String unit
) {
    public static ContractServiceFeeDto fromEntity(ContractServiceFee fee) {
        return new ContractServiceFeeDto(
            fee.getId(),
            fee.getServiceFeeId(),
            fee.getName(),
            fee.getFeeType().name(),
            fee.getUnitRate(),
            fee.getAmount(),
            fee.getUnit()
        );
    }
}
