package com.pegacorn.rently.dto.common;

/**
 * DTO for badge counts used in the notification badge system.
 * Returns counts instead of full data to optimize API performance.
 */
public record BadgeCountsDto(
        int draftContracts,
        int unresolvedTickets,
        int unpaidInvoices,
        boolean hasUtilityShortfall,
        boolean hasRentedRooms,
        int undoneTasks) {
}
