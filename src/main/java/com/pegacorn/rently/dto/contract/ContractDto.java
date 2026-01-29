package com.pegacorn.rently.dto.contract;

import com.pegacorn.rently.entity.Contract;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import java.util.List;

public record ContractDto(
        String id,
        String roomId,
        RoomInfo room,
        String landlordId,
        String tenantId,
        TenantInfo tenant,
        // Duration
        Integer duration,
        String durationUnit,
        String startDate,
        String endDate,
        // Payment
        String paymentPeriod,
        Integer paymentDueDay,
        BigDecimal monthlyRent,
        Integer depositMonths,
        BigDecimal depositAmount,
        boolean depositPaid,
        // Service fees
        List<ContractServiceFeeDto> serviceFees,
        // Template
        String templateId,
        String customTerms,
        boolean hasContentSnapshot, // True if contract content is frozen (activated)
        // Status
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record RoomInfo(String id, String code, String houseName, String houseId) {}
    public record TenantInfo(String id, String fullName, String phone) {}

    public static ContractDto fromEntity(Contract contract) {
        RoomInfo roomInfo = contract.getRoom() != null
                ? new RoomInfo(
                        contract.getRoom().getId(),
                        contract.getRoom().getCode(),
                        contract.getRoom().getHouseName(),
                        contract.getRoom().getHouseId())
                : null;

        TenantInfo tenantInfo = contract.getTenant() != null
                ? new TenantInfo(
                        contract.getTenant().getId(),
                        contract.getTenant().getFullName(),
                        contract.getTenant().getPhone())
                : null;

        List<ContractServiceFeeDto> serviceFees = contract.getServiceFees() != null
                ? contract.getServiceFees().stream().map(ContractServiceFeeDto::fromEntity).toList()
                : List.of();

        return new ContractDto(
                contract.getId(),
                contract.getRoomId(),
                roomInfo,
                contract.getLandlordId(),
                contract.getTenantId(),
                tenantInfo,
                contract.getDuration(),
                contract.getDurationUnit().name(),
                contract.getStartDate().toString(),
                contract.getEndDate().toString(),
                contract.getPaymentPeriod().name(),
                contract.getPaymentDueDay(),
                contract.getMonthlyRent(),
                contract.getDepositMonths(),
                contract.getDepositAmount(),
                contract.isDepositPaid(),
                serviceFees,
                contract.getTemplateId(),
                contract.getCustomTerms(),
                contract.getContentSnapshot() != null,
                contract.getStatus().name(),
                contract.getCreatedAt(),
                contract.getUpdatedAt()
        );
    }
}
