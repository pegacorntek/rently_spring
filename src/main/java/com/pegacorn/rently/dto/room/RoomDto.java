package com.pegacorn.rently.dto.room;

import com.pegacorn.rently.entity.Room;
import com.pegacorn.rently.entity.RoomTenant;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record RoomDto(
                String id,
                String houseId,
                String code,
                int floor,
                BigDecimal areaM2,
                BigDecimal baseRent,
                Integer maxTenants,
                String status,
                List<TenantDto> tenants,
                ContractDto currentContract,
                BigDecimal debt,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
        public record TenantDto(
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
                        LocalDateTime joinedAt) {
                public static TenantDto fromEntity(RoomTenant tenant) {
                        return new TenantDto(
                                        tenant.getId(),
                                        tenant.getUserId(),
                                        tenant.getFullName(),
                                        tenant.getPhone(),
                                        tenant.getIdNumber(),
                                        tenant.getIdIssueDate(),
                                        tenant.getIdIssuePlace(),
                                        tenant.getGender() != null ? tenant.getGender().name() : null,
                                        tenant.getDateOfBirth(),
                                        tenant.getPlaceOfOrigin(),
                                        tenant.isPrimary(),
                                        tenant.getJoinedAt());
                }
        }

        public record ContractDto(
                        String id,
                        String status,
                        String startDate,
                        String endDate,
                        BigDecimal monthlyRent,
                        BigDecimal deposit,
                        Integer paymentDueDay,
                        List<ServiceFeeDto> serviceFees) {
        }

        public record ServiceFeeDto(
                        String id,
                        String serviceFeeId,
                        String name,
                        String feeType,
                        BigDecimal unitRate,
                        BigDecimal amount,
                        String unit) {
        }

        public static RoomDto fromEntity(Room room) {
                List<TenantDto> tenantDtos = room.getTenants() != null
                                ? room.getTenants().stream().map(TenantDto::fromEntity).toList()
                                : null;

                ContractDto contractDto = null;
                if (room.getCurrentContract() != null) {
                        List<ServiceFeeDto> serviceFees = room.getCurrentContract().getServiceFees() != null
                                ? room.getCurrentContract().getServiceFees().stream()
                                        .map(sf -> new ServiceFeeDto(
                                                sf.getId(),
                                                sf.getServiceFeeId(),
                                                sf.getName(),
                                                sf.getFeeType().name(),
                                                sf.getUnitRate(),
                                                sf.getAmount(),
                                                sf.getUnit()))
                                        .toList()
                                : List.of();
                        contractDto = new ContractDto(
                                room.getCurrentContract().getId(),
                                room.getCurrentContract().getStatus().name(),
                                room.getCurrentContract().getStartDate().toString(),
                                room.getCurrentContract().getEndDate().toString(),
                                room.getCurrentContract().getMonthlyRent(),
                                room.getCurrentContract().getDepositAmount(),
                                room.getCurrentContract().getPaymentDueDay(),
                                serviceFees);
                }

                return new RoomDto(
                                room.getId(),
                                room.getHouseId(),
                                room.getCode(),
                                room.getFloor(),
                                room.getAreaM2(),
                                room.getBaseRent(),
                                room.getMaxTenants(),
                                room.getStatus().name(),
                                tenantDtos,
                                contractDto,
                                room.getDebt() != null ? room.getDebt() : BigDecimal.ZERO,
                                room.getCreatedAt(),
                                room.getUpdatedAt());
        }
}
