package com.pegacorn.rently.dto.servicefee;

import com.pegacorn.rently.entity.ServiceFee;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record CreateServiceFeeRequest(
        @NotBlank(message = "Tên phí dịch vụ không được để trống")
        @Size(max = 100, message = "Tên phí dịch vụ tối đa 100 ký tự")
        String name,

        @NotNull(message = "Loại phí không được để trống")
        ServiceFee.FeeType feeType,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0", message = "Số tiền phải >= 0")
        BigDecimal amount,

        @Size(max = 50, message = "Đơn vị tối đa 50 ký tự")
        String unit,

        Integer displayOrder
) {}
