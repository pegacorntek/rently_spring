package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.house.CreateHouseRequest;
import com.pegacorn.rently.dto.house.HouseDto;
import com.pegacorn.rently.dto.house.HouseStatsDto;
import com.pegacorn.rently.dto.house.UpdateHouseRequest;
import com.pegacorn.rently.entity.Amenity;
import com.pegacorn.rently.entity.House;
import com.pegacorn.rently.entity.ServiceFee;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.AmenityRepository;
import com.pegacorn.rently.repository.ExpenseRepository;
import com.pegacorn.rently.repository.HouseRepository;
import com.pegacorn.rently.repository.ServiceFeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class HouseService {

        private final HouseRepository houseRepository;
        private final AmenityRepository amenityRepository;
        private final ServiceFeeRepository serviceFeeRepository;
        private final ExpenseRepository expenseRepository;
        private final ActivityLogService activityLogService;
        private final TaskService taskService;

        public List<HouseDto> getAllByOwner(String ownerId) {
                List<House> houses = houseRepository.findByOwnerId(ownerId);
                return houses.stream()
                                .map(this::enrichHouse)
                                .map(HouseDto::fromEntity)
                                .toList();
        }

        public HouseDto getById(String id, String ownerId) {
                House house = houseRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

                if (!house.getOwnerId().equals(ownerId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                return HouseDto.fromEntity(enrichHouse(house));
        }

        @Transactional
        public HouseDto create(CreateHouseRequest request, String ownerId) {
                House house = House.builder()
                                .id(UUID.randomUUID().toString())
                                .ownerId(ownerId)
                                .name(request.name())
                                .address(request.address())
                                .description(request.description())
                                .status(House.HouseStatus.ACTIVE)
                                .createdAt(LocalDateTime.now())
                                .updatedAt(LocalDateTime.now())
                                .build();

                houseRepository.save(house);

                // Create service fees if provided
                if (request.serviceFees() != null && !request.serviceFees().isEmpty()) {
                        // Validate amounts before saving
                        BigDecimal maxAmount = new BigDecimal("9999999999.99");
                        for (var feeItem : request.serviceFees()) {
                                if (feeItem.amount() != null && feeItem.amount().compareTo(maxAmount) > 0) {
                                        throw ApiException.badRequest(
                                                        String.format("Số tiền phí '%s' vượt quá giới hạn cho phép",
                                                                        feeItem.name()));
                                }
                                if (feeItem.amount() != null && feeItem.amount().compareTo(BigDecimal.ZERO) < 0) {
                                        throw ApiException.badRequest(
                                                        String.format("Số tiền phí '%s' không được âm",
                                                                        feeItem.name()));
                                }
                        }

                        LocalDateTime now = LocalDateTime.now();
                        List<ServiceFee> serviceFees = request.serviceFees().stream()
                                        .map(feeItem -> ServiceFee.builder()
                                                        .id(UUID.randomUUID().toString())
                                                        .houseId(house.getId())
                                                        .name(feeItem.name())
                                                        .feeType(feeItem.feeType() != null ? feeItem.feeType()
                                                                        : ServiceFee.FeeType.FIXED)
                                                        .amount(feeItem.amount() != null ? feeItem.amount()
                                                                        : BigDecimal.ZERO)
                                                        .unit(feeItem.unit())
                                                        .displayOrder(feeItem.displayOrder())
                                                        .isActive(true)
                                                        .createdAt(now)
                                                        .updatedAt(now)
                                                        .build())
                                        .toList();
                        serviceFeeRepository.saveAll(serviceFees);
                }

                // Create default amenities
                createDefaultAmenities(house.getId());

                // Create default task to add room
                taskService.createDefaultTask("Thêm phòng cho " + house.getName(), ownerId);

                // Log activity
                activityLogService.logHouseCreated(ownerId, house.getId(), house.getName());

                return HouseDto.fromEntity(enrichHouse(house));
        }

        private void createDefaultAmenities(String houseId) {
                LocalDateTime now = LocalDateTime.now();

                List<Amenity> defaultAmenities = List.of(
                                // FURNITURE
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Bàn")
                                                .category(Amenity.AmenityCategory.FURNITURE)
                                                .icon("desk")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Ghế")
                                                .category(Amenity.AmenityCategory.FURNITURE)
                                                .icon("chair")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Giường")
                                                .category(Amenity.AmenityCategory.FURNITURE)
                                                .icon("bed")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Tủ lạnh")
                                                .category(Amenity.AmenityCategory.FURNITURE)
                                                .icon("kitchen")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Tủ quần áo")
                                                .category(Amenity.AmenityCategory.FURNITURE)
                                                .icon("door_sliding")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                // APPLIANCE
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Điều hòa")
                                                .category(Amenity.AmenityCategory.APPLIANCE)
                                                .icon("ac_unit")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Bình nóng lạnh")
                                                .category(Amenity.AmenityCategory.APPLIANCE)
                                                .icon("water_heater")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Máy giặt (riêng)")
                                                .category(Amenity.AmenityCategory.APPLIANCE)
                                                .icon("local_laundry_service")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Quạt")
                                                .category(Amenity.AmenityCategory.APPLIANCE)
                                                .icon("air")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                // FACILITY
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Thang máy")
                                                .category(Amenity.AmenityCategory.FACILITY)
                                                .icon("elevator")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build(),
                                Amenity.builder()
                                                .id(UUID.randomUUID().toString())
                                                .houseId(houseId)
                                                .name("Máy giặt (chung)")
                                                .category(Amenity.AmenityCategory.FACILITY)
                                                .icon("local_laundry_service")
                                                .isCustom(false)
                                                .createdAt(now)
                                                .build());

                amenityRepository.saveAll(defaultAmenities);
        }

        @Transactional
        public HouseDto update(String id, UpdateHouseRequest request, String ownerId) {
                House house = houseRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

                if (!house.getOwnerId().equals(ownerId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                if (request.name() != null) {
                        house.setName(request.name());
                }
                if (request.address() != null) {
                        house.setAddress(request.address());
                }
                if (request.description() != null) {
                        house.setDescription(request.description());
                }
                if (request.status() != null) {
                        house.setStatus(request.status());
                }
                house.setUpdatedAt(LocalDateTime.now());

                houseRepository.save(house);
                return HouseDto.fromEntity(enrichHouse(house));
        }

        @Transactional
        public void delete(String id, String ownerId) {
                House house = houseRepository.findById(id)
                                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

                if (!house.getOwnerId().equals(ownerId)) {
                        throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
                }

                houseRepository.delete(house);
        }

        private House enrichHouse(House house) {
                house.setRoomCount(houseRepository.countRoomsByHouseId(house.getId()));
                house.setTenantCount(houseRepository.countTenantsByHouseId(house.getId()));
                return house;
        }

        public List<HouseStatsDto> getStats(String ownerId, Integer month, Integer year) {
                List<House> houses = houseRepository.findByOwnerId(ownerId);
                LocalDate expiryDate = LocalDate.now().plusDays(30);

                // Build period filter if month/year provided
                String periodMonth = null;
                LocalDate startDate = null;
                LocalDate endDate = null;
                if (month != null && year != null) {
                        periodMonth = String.format("%d-%02d", year, month);
                        startDate = LocalDate.of(year, month, 1);
                        endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
                }

                final String finalPeriodMonth = periodMonth;
                final LocalDate finalStartDate = startDate;
                final LocalDate finalEndDate = endDate;

                return houses.stream()
                                .map(house -> {
                                        String houseId = house.getId();
                                        int totalRooms = houseRepository.countRoomsByHouseId(houseId);
                                        int rentedRooms = houseRepository.countRentedRoomsByHouseId(houseId);
                                        int vacantRooms = houseRepository.countVacantRoomsByHouseId(houseId);
                                        int debtRooms = houseRepository.countDebtRoomsByHouseId(houseId);
                                        int expiringContracts = houseRepository.countExpiringContractsByHouseId(houseId,
                                                        expiryDate);
                                        int totalTenants = houseRepository.countTenantsByHouseId(houseId);
                                        int missingInfoTenants = houseRepository
                                                        .countMissingInfoTenantsByHouseId(houseId);
                                        BigDecimal deposit = houseRepository.sumDepositByHouseId(houseId);

                                        // Use period-filtered queries if month/year provided
                                        BigDecimal debt;
                                        BigDecimal paid;
                                        BigDecimal expense;
                                        if (finalPeriodMonth != null) {
                                                debt = houseRepository.sumDebtByHouseIdAndPeriod(houseId, finalPeriodMonth);
                                                paid = houseRepository.sumPaidByHouseIdAndPeriod(houseId, finalPeriodMonth);
                                                expense = expenseRepository.sumAmountByHouseIdAndDateRange(houseId, finalStartDate, finalEndDate);
                                        } else {
                                                debt = houseRepository.sumDebtByHouseId(houseId);
                                                paid = houseRepository.sumPaidByHouseId(houseId);
                                                expense = expenseRepository.sumAmountByHouseId(houseId);
                                        }

                                        return new HouseStatsDto(
                                                        houseId,
                                                        house.getName(),
                                                        totalRooms,
                                                        rentedRooms,
                                                        vacantRooms,
                                                        debtRooms,
                                                        expiringContracts,
                                                        totalTenants,
                                                        0, // unregisteredTenants - not tracked yet
                                                        missingInfoTenants,
                                                        deposit != null ? deposit : BigDecimal.ZERO,
                                                        debt != null ? debt : BigDecimal.ZERO,
                                                        paid != null ? paid : BigDecimal.ZERO,
                                                        expense != null ? expense : BigDecimal.ZERO);
                                })
                                .toList();
        }
}
