package com.pegacorn.rently.dto.room;

import com.pegacorn.rently.entity.Room;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

import java.math.BigDecimal;

public record UpdateRoomRequest(
        String code,

        @Min(value = 1, message = "Floor must be at least 1")
        Integer floor,

        @DecimalMin(value = "0.01", message = "Area must be greater than 0")
        BigDecimal areaM2,

        @DecimalMin(value = "0", message = "Base rent cannot be negative")
        BigDecimal baseRent,

        @Min(value = 1, message = "Max tenants must be at least 1")
        Integer maxTenants,

        Room.RoomStatus status
) {}
