package com.pegacorn.rently.dto.amenity;

import com.pegacorn.rently.entity.Amenity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAmenityRequest(
        String houseId,
        @NotBlank(message = "Name is required")
        String name,
        @NotNull(message = "Category is required")
        Amenity.AmenityCategory category,
        String icon,
        BigDecimal price
) {}
