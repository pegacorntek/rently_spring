package com.pegacorn.rently.dto.amenity;

import jakarta.validation.constraints.NotBlank;

public record AddRoomAmenityRequest(
                @NotBlank(message = "Amenity ID is required") String amenityId,
                Integer quantity,
                String notes,
                String condition) {
}
