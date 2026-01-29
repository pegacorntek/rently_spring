package com.pegacorn.rently.dto.ai;

import java.util.List;
import java.util.Map;

/**
 * DTO containing all data fetched based on AI's requirements.
 * This is sent to the second AI pass for processing.
 */
public record FetchedDataDto(
    List<HouseData> houses,
    List<RoomData> rooms,
    List<ContractData> contracts,
    List<InvoiceData> invoices,
    List<ExpenseData> expenses,
    List<MeterData> meterReadings,
    SummaryData summary
) {
    public record HouseData(
        String id,
        String name,
        String address,
        int totalRooms,
        int emptyRooms,
        int rentedRooms
    ) {}

    public record RoomData(
        String id,
        String code,
        String houseName,
        String status,
        java.math.BigDecimal baseRent,
        String tenantName
    ) {}

    public record ContractData(
        String id,
        String roomCode,
        String houseName,
        String tenantName,
        String status,
        java.math.BigDecimal monthlyRent,
        String startDate,
        String endDate
    ) {}

    public record InvoiceData(
        String id,
        String roomCode,
        String tenantName,
        String periodMonth,
        java.math.BigDecimal totalAmount,
        java.math.BigDecimal paidAmount,
        String status,
        String dueDate
    ) {}

    public record ExpenseData(
        String id,
        String title,
        String category,
        String houseName,
        java.math.BigDecimal amount,
        String status,
        String expenseDate
    ) {}

    public record MeterData(
        String roomCode,
        String periodMonth,
        java.math.BigDecimal electricityOld,
        java.math.BigDecimal electricityNew,
        java.math.BigDecimal waterOld,
        java.math.BigDecimal waterNew
    ) {}

    public record SummaryData(
        int totalHouses,
        int totalRooms,
        int emptyRooms,
        int rentedRooms,
        java.math.BigDecimal totalInvoiceAmount,
        java.math.BigDecimal paidInvoiceAmount,
        java.math.BigDecimal totalExpenseAmount
    ) {}

    public static FetchedDataDto empty() {
        return new FetchedDataDto(
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(),
            new SummaryData(0, 0, 0, 0,
                java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO, java.math.BigDecimal.ZERO)
        );
    }
}
