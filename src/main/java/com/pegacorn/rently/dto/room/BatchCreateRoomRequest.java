package com.pegacorn.rently.dto.room;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.math.BigDecimal;
import java.util.List;

public record BatchCreateRoomRequest(
        @NotBlank(message = "House ID is required")
        String houseId,

        @NotEmpty(message = "At least one room is required")
        @Valid
        List<RoomItem> rooms
) {
    public record RoomItem(
            @NotBlank(message = "Room code is required")
            String code,

            int floor,

            BigDecimal areaM2,

            BigDecimal baseRent,

            Integer maxTenants
    ) {}
}
