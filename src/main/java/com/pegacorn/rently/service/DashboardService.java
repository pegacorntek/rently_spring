package com.pegacorn.rently.service;

import com.pegacorn.rently.dto.landlord.DashboardSummaryDto;
import com.pegacorn.rently.dto.common.BadgeCountsDto;
import com.pegacorn.rently.dto.expense.ExpenseSummaryDto;
import com.pegacorn.rently.dto.invoice.InvoiceSummaryDto;
import com.pegacorn.rently.entity.Room;
import com.pegacorn.rently.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {
    private final ActivityLogService activityLogService;
    private final ContractService contractService;
    private final ExpenseService expenseService;
    private final InvoiceService invoiceService;
    private final TaskService taskService;
    private final TicketService ticketService;
    private final RoomRepository roomRepository;

    public DashboardSummaryDto getSummary(String houseId, String userId) {
        LocalDate today = LocalDate.now();
        int month = today.getMonthValue();
        int year = today.getYear();

        // 1. Badge Counts
        BadgeCountsDto badgeCounts = getBadgeCounts(houseId, userId);

        // 2. Expense Summary
        ExpenseSummaryDto expenseSummary = expenseService.getSummary(userId, houseId, month, year);

        // 3. Invoice Summary
        InvoiceSummaryDto invoiceSummary = invoiceService.getSummary(userId, houseId, month, year);

        // 4. Tasks
        var tasks = taskService.getAll(userId);

        // 5. Recent Activities
        var activities = activityLogService.getRecentActivities(userId, 10);

        // 6. Stats
        DashboardSummaryDto.DashboardStatsDto stats = getDashboardStats(houseId, userId);

        return new DashboardSummaryDto(
                badgeCounts,
                expenseSummary,
                invoiceSummary,
                tasks,
                activities,
                stats);
    }

    private BadgeCountsDto getBadgeCounts(String houseId, String userId) {
        int draftContractsCount = (int) contractService.getAllByLandlord(userId, houseId, "DRAFT").size();

        var allTickets = ticketService.getAllByLandlord(userId, houseId, null);
        int unresolvedTicketsCount = (int) allTickets.stream()
                .filter(t -> "OPEN".equals(t.status()) || "IN_PROGRESS".equals(t.status()))
                .count();

        int unpaidInvoicesCount = 0;
        LocalDate today = LocalDate.now();
        if (today.getDayOfMonth() > 25) {
            var allInvoices = invoiceService.getAllByLandlord(userId, houseId, null, null, null);
            unpaidInvoicesCount = (int) allInvoices.stream()
                    .filter(inv -> "DRAFT".equals(inv.status()) || "PARTIALLY_PAID".equals(inv.status())
                            || "OVERDUE".equals(inv.status()))
                    .count();
        }

        boolean hasShortfall = false;
        if (houseId != null) {
            try {
                var reconciliation = invoiceService.getUtilityReconciliation(
                        userId, houseId, today.getMonthValue(), today.getYear());
                hasShortfall = reconciliation.totalShortfall().compareTo(java.math.BigDecimal.ZERO) > 0;
            } catch (Exception e) {
                // Ignore
            }
        }

        // Check if there are any rented rooms
        boolean hasRentedRooms = false;
        if (houseId != null) {
            var rooms = roomRepository.findByHouseId(houseId);
            hasRentedRooms = rooms.stream().anyMatch(r -> r.getStatus() == Room.RoomStatus.RENTED);
        }

        // Count undone tasks
        var allTasks = taskService.getAll(userId);
        int undoneTasksCount = (int) allTasks.stream().filter(t -> !t.isDone()).count();

        return new BadgeCountsDto(
                draftContractsCount,
                unresolvedTicketsCount,
                unpaidInvoicesCount,
                hasShortfall,
                hasRentedRooms,
                undoneTasksCount);
    }

    private DashboardSummaryDto.DashboardStatsDto getDashboardStats(String houseId, String userId) {
        List<Room> rooms;
        if (houseId != null) {
            rooms = roomRepository.findByHouseId(houseId);
        } else {
            rooms = roomRepository.findAllByLandlordId(userId);
        }

        int totalRooms = rooms.size();
        int occupiedRooms = (int) rooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.RENTED).count();
        int emptyRooms = (int) rooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.EMPTY).count();
        int reservedRooms = (int) rooms.stream().filter(r -> r.getStatus() == Room.RoomStatus.RESERVED).count();

        // Calculate rooms with debt
        int roomsWithDebt = 0;
        for (Room room : rooms) {
            if (roomRepository.calculateDebtByRoomId(room.getId()).compareTo(java.math.BigDecimal.ZERO) > 0) {
                roomsWithDebt++;
            }
        }

        return new DashboardSummaryDto.DashboardStatsDto(
                totalRooms,
                occupiedRooms,
                emptyRooms,
                reservedRooms,
                roomsWithDebt);
    }
}
