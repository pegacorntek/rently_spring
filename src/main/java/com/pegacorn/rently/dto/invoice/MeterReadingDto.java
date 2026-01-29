package com.pegacorn.rently.dto.invoice;

import com.pegacorn.rently.entity.MeterReading;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record MeterReadingDto(
        @NotBlank(message = "Vui lòng chọn phòng")
        String roomId,

        @NotBlank(message = "Vui lòng chọn kỳ ghi")
        String periodMonth,

        @NotNull(message = "Vui lòng nhập chỉ số điện cũ")
        @DecimalMin(value = "0", message = "Chỉ số điện không thể âm")
        BigDecimal electricityOld,

        @NotNull(message = "Vui lòng nhập chỉ số điện mới")
        @DecimalMin(value = "0", message = "Chỉ số điện không thể âm")
        BigDecimal electricityNew,

        // Optional - uses default if not provided
        @DecimalMin(value = "0", message = "Đơn giá không thể âm")
        BigDecimal electricityUnitPrice,

        @NotNull(message = "Vui lòng nhập chỉ số nước cũ")
        @DecimalMin(value = "0", message = "Chỉ số nước không thể âm")
        BigDecimal waterOld,

        @NotNull(message = "Vui lòng nhập chỉ số nước mới")
        @DecimalMin(value = "0", message = "Chỉ số nước không thể âm")
        BigDecimal waterNew,

        // Optional - uses default if not provided
        @DecimalMin(value = "0", message = "Đơn giá không thể âm")
        BigDecimal waterUnitPrice
) {
    public static MeterReadingDto fromEntity(MeterReading reading) {
        return new MeterReadingDto(
                reading.getRoomId(),
                reading.getPeriodMonth(),
                reading.getElectricityOld(),
                reading.getElectricityNew(),
                reading.getElectricityUnitPrice(),
                reading.getWaterOld(),
                reading.getWaterNew(),
                reading.getWaterUnitPrice()
        );
    }
}
