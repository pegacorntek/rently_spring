package com.pegacorn.rently.dto.amenity;

public record UpdateRoomAmenityRequest(
                Integer quantity,
                String notes,
                String condition) {
}
