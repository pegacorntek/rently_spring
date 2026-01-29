package com.pegacorn.rently.dto.landlord;

import com.pegacorn.rently.dto.activity.ActivityLogDto;
import com.pegacorn.rently.dto.common.BadgeCountsDto;
import com.pegacorn.rently.dto.expense.ExpenseSummaryDto;
import com.pegacorn.rently.dto.invoice.InvoiceSummaryDto;
import com.pegacorn.rently.dto.task.TaskDto;
import java.util.List;

public record DashboardSummaryDto(
        BadgeCountsDto badgeCounts,
        ExpenseSummaryDto expenseSummary,
        InvoiceSummaryDto invoiceSummary,
        List<TaskDto> tasks,
        List<ActivityLogDto> recentActivities,
        DashboardStatsDto stats) {
    public record DashboardStatsDto(
            int totalRooms,
            int occupiedRooms,
            int emptyRooms,
            int reservedRooms,
            int roomsWithDebt) {
    }
}
