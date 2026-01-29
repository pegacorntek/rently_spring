package com.pegacorn.rently.dto.room;

import java.util.List;

public record BatchCreateRoomResponse(
        int totalRequested,
        int successCount,
        int failedCount,
        List<RoomDto> createdRooms,
        List<FailedRoom> failedRooms
) {
    public record FailedRoom(
            String code,
            String reason
    ) {}
}
