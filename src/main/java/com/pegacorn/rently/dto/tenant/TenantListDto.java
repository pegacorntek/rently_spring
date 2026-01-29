package com.pegacorn.rently.dto.tenant;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record TenantListDto(
        String id,
        String userId,
        String fullName,
        String phone,
        String idNumber,
        LocalDate idIssueDate,
        String idIssuePlace,
        String gender,
        LocalDate dateOfBirth,
        String placeOfOrigin,
        boolean isPrimary,
        LocalDateTime joinedAt,
        RoomInfo room,
        HouseInfo house
) {
    public record RoomInfo(
            String id,
            String code,
            int floor
    ) {}

    public record HouseInfo(
            String id,
            String name,
            String address
    ) {}
}
