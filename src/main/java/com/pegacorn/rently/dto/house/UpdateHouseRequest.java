package com.pegacorn.rently.dto.house;

import com.pegacorn.rently.entity.House;
import jakarta.validation.constraints.Size;

public record UpdateHouseRequest(
        @Size(max = 100, message = "House name must be less than 100 characters")
        String name,

        @Size(max = 255, message = "Address must be less than 255 characters")
        String address,

        String description,

        House.HouseStatus status
) {}
