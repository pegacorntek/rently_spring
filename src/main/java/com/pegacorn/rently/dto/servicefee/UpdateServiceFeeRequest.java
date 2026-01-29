package com.pegacorn.rently.dto.servicefee;

import com.pegacorn.rently.entity.ServiceFee;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateServiceFeeRequest(
        @Size(max = 100, message = "Tên phí dịch vụ tối đa 100 ký tự")
        String name,

        ServiceFee.FeeType feeType,

        @DecimalMin(value = "0", message = "Số tiền phải >= 0")
        BigDecimal amount,

        @Size(max = 50, message = "Đơn vị tối đa 50 ký tự")
        String unit,

        Boolean isActive,

        Integer displayOrder
) {}
