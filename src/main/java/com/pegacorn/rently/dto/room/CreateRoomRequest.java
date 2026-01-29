package com.pegacorn.rently.dto.room;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateRoomRequest(
        @NotBlank(message = "House ID is required")
        String houseId,

        @NotBlank(message = "Room code is required")
        String code,

        @Min(value = 1, message = "Floor must be at least 1")
        int floor,

        @NotNull(message = "Area is required")
        @DecimalMin(value = "0.01", message = "Area must be greater than 0")
        BigDecimal areaM2,

        @NotNull(message = "Base rent is required")
        @DecimalMin(value = "0", message = "Base rent cannot be negative")
        BigDecimal baseRent,

        @Min(value = 1, message = "Max tenants must be at least 1")
        Integer maxTenants
) {}
