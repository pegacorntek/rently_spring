package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.room.*;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RoomService {

    private final RoomRepository roomRepository;
    private final RoomTenantRepository roomTenantRepository;
    private final HouseRepository houseRepository;
    private final ContractRepository contractRepository;
    private final ContractServiceFeeRepository contractServiceFeeRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final ActivityLogService activityLogService;
    private final SmsService smsService;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    public List<RoomDto> getAll(String ownerId) {
        List<Room> rooms = roomRepository.findAllByLandlordId(ownerId);
        return rooms.stream()
                .map(this::enrichRoom)
                .map(RoomDto::fromEntity)
                .toList();
    }

    public List<RoomDto> getByHouse(String houseId, String ownerId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        List<Room> rooms = roomRepository.findByHouseId(houseId);
        return rooms.stream()
                .map(this::enrichRoom)
                .map(RoomDto::fromEntity)
                .toList();
    }

    public Page<RoomDto> getByHousePaginated(String houseId, String ownerId, Pageable pageable) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        Page<Room> rooms = roomRepository.findByHouseIdOrderByCodeAsc(houseId, pageable);
        return rooms.map(room -> RoomDto.fromEntity(enrichRoom(room)));
    }

    public RoomDto getById(String id, String ownerId) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return RoomDto.fromEntity(enrichRoom(room));
    }

    @Transactional
    public RoomDto create(CreateRoomRequest request, String ownerId) {
        House house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (roomRepository.existsByHouseIdAndCode(request.houseId(), request.code())) {
            throw ApiException.conflict(MessageConstant.ROOM_CODE_EXISTS_IN_HOUSE);
        }

        Room room = Room.builder()
                .id(UUID.randomUUID().toString())
                .houseId(request.houseId())
                .code(request.code())
                .floor(request.floor())
                .areaM2(request.areaM2())
                .baseRent(request.baseRent())
                .maxTenants(request.maxTenants() != null ? request.maxTenants() : 2)
                .status(Room.RoomStatus.EMPTY)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        roomRepository.save(room);

        // Log activity
        activityLogService.logRoomCreated(ownerId, room.getId(), room.getCode(), house.getName());

        return RoomDto.fromEntity(enrichRoom(room));
    }

    @Transactional
    public BatchCreateRoomResponse createBatch(BatchCreateRoomRequest request, String ownerId) {
        House house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        List<RoomDto> createdRooms = new ArrayList<>();
        List<BatchCreateRoomResponse.FailedRoom> failedRooms = new ArrayList<>();

        for (BatchCreateRoomRequest.RoomItem item : request.rooms()) {
            try {
                if (roomRepository.existsByHouseIdAndCode(request.houseId(), item.code())) {
                    failedRooms.add(new BatchCreateRoomResponse.FailedRoom(
                            item.code(), MessageConstant.ROOM_CODE_EXISTS));
                    continue;
                }

                Room room = Room.builder()
                        .id(UUID.randomUUID().toString())
                        .houseId(request.houseId())
                        .code(item.code())
                        .floor(item.floor())
                        .areaM2(item.areaM2() != null ? item.areaM2() : BigDecimal.ZERO)
                        .baseRent(item.baseRent() != null ? item.baseRent() : BigDecimal.ZERO)
                        .maxTenants(item.maxTenants() != null ? item.maxTenants() : 2)
                        .status(Room.RoomStatus.EMPTY)
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .build();

                roomRepository.save(room);
                createdRooms.add(RoomDto.fromEntity(enrichRoom(room)));
            } catch (Exception e) {
                failedRooms.add(new BatchCreateRoomResponse.FailedRoom(
                        item.code(), e.getMessage()));
            }
        }

        return new BatchCreateRoomResponse(
                request.rooms().size(),
                createdRooms.size(),
                failedRooms.size(),
                createdRooms,
                failedRooms);
    }

    @Transactional
    public RoomDto update(String id, UpdateRoomRequest request, String ownerId) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (request.code() != null && !request.code().equals(room.getCode())) {
            if (roomRepository.existsByHouseIdAndCode(room.getHouseId(), request.code())) {
                throw ApiException.conflict(MessageConstant.ROOM_CODE_EXISTS);
            }
            room.setCode(request.code());
        }
        if (request.floor() != null) {
            room.setFloor(request.floor());
        }
        if (request.areaM2() != null) {
            room.setAreaM2(request.areaM2());
        }
        if (request.baseRent() != null) {
            room.setBaseRent(request.baseRent());
        }
        if (request.maxTenants() != null) {
            room.setMaxTenants(request.maxTenants());
        }
        if (request.status() != null && request.status() != room.getStatus()) {
            // Block changing to MAINTENANCE if room has active contract
            if (request.status() == Room.RoomStatus.MAINTENANCE) {
                boolean hasActiveContract = contractRepository
                        .findByRoomIdAndStatus(room.getId(), Contract.ContractStatus.ACTIVE).isPresent();
                if (hasActiveContract) {
                    throw ApiException.conflict(MessageConstant.CANNOT_MAINTENANCE_WITH_CONTRACT);
                }
            }
            String oldStatus = room.getStatus().name();
            room.setStatus(request.status());
            activityLogService.logRoomStatusChanged(ownerId, room.getId(), room.getCode(), oldStatus,
                    request.status().name());
        }
        room.setUpdatedAt(LocalDateTime.now());

        roomRepository.save(room);
        return RoomDto.fromEntity(enrichRoom(room));
    }

    @Transactional
    public void delete(String id, String ownerId) {
        Room room = roomRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (room.getStatus() == Room.RoomStatus.RENTED) {
            throw ApiException.conflict(MessageConstant.CANNOT_DELETE_RENTED_ROOM);
        }

        roomRepository.delete(room);
    }

    @Transactional
    public void addTenant(String roomId, AddTenantRequest request, String ownerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Check for duplicate ID number if provided
        if (request.idNumber() != null && !request.idNumber().isBlank()) {
            userRepository.findByIdNumber(request.idNumber()).ifPresent(existingUser -> {
                // If existing user has different phone, it's a duplicate
                if (!existingUser.getPhone().equals(request.phone())) {
                    throw ApiException.conflict(MessageConstant.ID_NUMBER_CONFLICT);
                }
            });
        }

        // Find existing user by phone or ID number, or create new one
        boolean isNewTenant = false;
        User tenant = userRepository.findByPhone(request.phone())
                .or(() -> request.idNumber() != null ? userRepository.findByIdNumber(request.idNumber())
                        : Optional.empty())
                .orElse(null);

        if (tenant == null) {
            tenant = createNewTenant(request);
            isNewTenant = true;
        }

        // Update existing user info if needed
        if (!isNewTenant && tenant.getIdNumber() == null && request.idNumber() != null && !request.idNumber().isBlank()) {
            tenant.setIdNumber(request.idNumber());
            tenant.setFullName(request.fullName() != null ? request.fullName() : tenant.getFullName());
            tenant.setGender(parseGender(request.gender()));
            tenant.setDateOfBirth(parseDate(request.dateOfBirth()));
            tenant.setPlaceOfOrigin(request.placeOfOrigin());
            tenant.setPlaceOfResidence(request.placeOfResidence());
            tenant.setIdIssueDate(parseDate(request.idIssueDate()));
            tenant.setIdIssuePlace(request.idIssuePlace());
            tenant.setUpdatedAt(LocalDateTime.now());
            userRepository.save(tenant);
        }

        if (roomTenantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, tenant.getId())) {
            throw ApiException.conflict(MessageConstant.TENANT_ALREADY_IN_ROOM);
        }

        // Ensure user has TENANT role
        if (!userRoleRepository.existsByUserIdAndRole(tenant.getId(), User.Role.TENANT)) {
            UserRole role = UserRole.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(tenant.getId())
                    .role(User.Role.TENANT)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(role);
        }

        // Determine if this tenant should be primary
        boolean shouldBePrimary;
        if (Boolean.TRUE.equals(request.isPrimary())) {
            // User explicitly requested primary - clear existing and set this one
            roomTenantRepository.clearPrimaryForRoom(roomId);
            shouldBePrimary = true;
        } else {
            // Auto-set as primary only if no existing primary
            boolean hasPrimary = roomTenantRepository.findActiveByRoomId(roomId).stream()
                    .anyMatch(RoomTenant::isPrimary);
            shouldBePrimary = !hasPrimary;
        }

        RoomTenant roomTenant = RoomTenant.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .userId(tenant.getId())
                .isPrimary(shouldBePrimary)
                .joinedAt(LocalDateTime.now())
                .build();

        roomTenantRepository.save(roomTenant);

        // Log tenant added
        activityLogService.logTenantAdded(ownerId, roomId, room.getCode(), tenant.getFullName(), tenant.getPhone());

        // Send welcome SMS to newly created tenants
        if (isNewTenant && request.idNumber() != null && !request.idNumber().isBlank()) {
            try {
                String loginUrl = appBaseUrl + "/login";
                smsService.sendTenantWelcome(
                        tenant.getPhone(),
                        tenant.getFullName(),
                        room.getCode(),
                        house.getName(),
                        loginUrl,
                        request.idNumber() // Password is ID number for new tenants
                );
            } catch (Exception e) {
                log.warn("Failed to send welcome SMS to tenant {}: {}", tenant.getPhone(), e.getMessage());
            }
        }

        // Update room status to RENTED when tenant is added (even without contract)
        // The "No contract" badge will be shown on frontend for rooms without contracts
        if (room.getStatus() == Room.RoomStatus.EMPTY) {
            String oldStatus = room.getStatus().name();
            room.setStatus(Room.RoomStatus.RENTED);
            room.setUpdatedAt(LocalDateTime.now());
            roomRepository.save(room);
            activityLogService.logRoomStatusChanged(ownerId, roomId, room.getCode(), oldStatus, "RENTED");
        }
    }

    private User createNewTenant(AddTenantRequest request) {
        if (request.idNumber() == null || request.idNumber().isBlank()) {
            throw ApiException.badRequest(MessageConstant.ID_NUMBER_REQUIRED);
        }
        if (request.fullName() == null || request.fullName().isBlank()) {
            throw ApiException.badRequest(MessageConstant.FULL_NAME_REQUIRED);
        }

        // Double-check ID number is not taken
        if (userRepository.existsByIdNumber(request.idNumber())) {
            throw ApiException.conflict(MessageConstant.ID_NUMBER_CONFLICT);
        }

        User user = User.builder()
                .id(UUID.randomUUID().toString())
                .phone(request.phone())
                .passwordHash(passwordEncoder.encode(request.idNumber()))
                .fullName(request.fullName())
                .idNumber(request.idNumber())
                .gender(parseGender(request.gender()))
                .dateOfBirth(parseDate(request.dateOfBirth()))
                .placeOfOrigin(request.placeOfOrigin())
                .placeOfResidence(request.placeOfResidence())
                .idIssueDate(parseDate(request.idIssueDate()))
                .idIssuePlace(request.idIssuePlace())
                .status(User.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        return userRepository.save(user);
    }

    private User.Gender parseGender(String gender) {
        if (gender == null || gender.isBlank())
            return null;
        try {
            return User.Gender.valueOf(gender.toLowerCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank())
            return null;
        try {
            // Support DD-MM-YYYY format
            return LocalDate.parse(dateStr);
        } catch (Exception e) {
            return null;
        }
    }

    @Transactional
    public void removeTenant(String roomId, String tenantId, String ownerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Check if there's an active contract for this room with this tenant as the
        // primary
        contractRepository.findByRoomIdAndStatus(roomId, Contract.ContractStatus.ACTIVE)
                .ifPresent(contract -> {
                    if (contract.getTenantId().equals(tenantId)) {
                        throw ApiException.badRequest(MessageConstant.CANNOT_REMOVE_CONTRACT_TENANT);
                    }
                });

        roomTenantRepository.markAsLeft(roomId, tenantId, LocalDateTime.now());

        // Check if room has any remaining tenants, if not set status to EMPTY
        // But only if there's no active contract
        List<RoomTenant> remainingTenants = roomTenantRepository.findActiveByRoomId(roomId);
        boolean hasActiveContract = contractRepository.existsByRoomIdAndStatus(roomId, Contract.ContractStatus.ACTIVE);
        if (remainingTenants.isEmpty() && room.getStatus() == Room.RoomStatus.RENTED && !hasActiveContract) {
            room.setStatus(Room.RoomStatus.EMPTY);
            room.setUpdatedAt(LocalDateTime.now());
            roomRepository.save(room);
            activityLogService.logRoomStatusChanged(ownerId, roomId, room.getCode(), "RENTED", "EMPTY");
        }
    }

    @Transactional
    public void setPrimaryTenant(String roomId, String userId, String ownerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Verify tenant exists in room
        if (!roomTenantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, userId)) {
            throw ApiException.notFound(MessageConstant.TENANT_NOT_FOUND_IN_ROOM);
        }

        // Clear all primary flags for this room, then set the new one
        roomTenantRepository.clearPrimaryForRoom(roomId);
        roomTenantRepository.setPrimary(roomId, userId);
    }

    private Room enrichRoom(Room room) {
        List<RoomTenant> tenants = roomTenantRepository.findActiveByRoomId(room.getId());
        room.setTenants(tenants);
        room.setDebt(roomRepository.calculateDebtByRoomId(room.getId()));

        // Get current contract (ACTIVE first, then DRAFT if no active)
        List<Contract> currentContracts = contractRepository.findCurrentContractsByRoomId(room.getId());
        if (!currentContracts.isEmpty()) {
            Contract contract = currentContracts.get(0);
            // Load contract service fees
            contract.setServiceFees(contractServiceFeeRepository.findByContractId(contract.getId()));
            room.setCurrentContract(contract);
        }

        // Auto-sync room status based on tenant count
        // Rooms with tenants should be RENTED, rooms without tenants should be EMPTY
        // The "No contract" badge will be shown on frontend for RENTED rooms without
        // contracts
        boolean hasTenants = !tenants.isEmpty();

        if (hasTenants && room.getStatus() == Room.RoomStatus.EMPTY) {
            room.setStatus(Room.RoomStatus.RENTED);
            room.setUpdatedAt(LocalDateTime.now());
            roomRepository.save(room);
        } else if (!hasTenants && room.getStatus() == Room.RoomStatus.RENTED) {
            room.setStatus(Room.RoomStatus.EMPTY);
            room.setUpdatedAt(LocalDateTime.now());
            roomRepository.save(room);
        }

        return room;
    }
}
