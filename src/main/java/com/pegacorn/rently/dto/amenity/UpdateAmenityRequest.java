package com.pegacorn.rently.dto.amenity;

import com.pegacorn.rently.entity.Amenity;

import java.math.BigDecimal;

public record UpdateAmenityRequest(
        String name,
        Amenity.AmenityCategory category,
        String icon,
        BigDecimal price
) {}
