package com.pegacorn.rently.dto.house;

import com.pegacorn.rently.entity.ServiceFee;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateHouseRequest(
                @NotBlank(message = "House name is required") @Size(max = 100, message = "House name must be less than 100 characters") String name,

                @NotBlank(message = "Address is required") @Size(max = 255, message = "Address must be less than 255 characters") String address,

                String description,

                @Valid List<ServiceFeeItem> serviceFees) {
        public record ServiceFeeItem(
                        @NotBlank(message = "Tên phí dịch vụ không được để trống") @Size(max = 100, message = "Tên phí dịch vụ tối đa 100 ký tự") String name,

                        ServiceFee.FeeType feeType,

                        @DecimalMin(value = "0", message = "Số tiền phải >= 0") BigDecimal amount,

                        @Size(max = 50, message = "Đơn vị tối đa 50 ký tự") String unit,

                        Integer displayOrder) {
        }
}
