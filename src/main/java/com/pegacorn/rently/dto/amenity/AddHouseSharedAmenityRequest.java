package com.pegacorn.rently.dto.amenity;

import jakarta.validation.constraints.NotBlank;

public record AddHouseSharedAmenityRequest(
        @NotBlank(message = "Amenity ID is required") String amenityId,
        Integer quantity,
        String notes) {
}
