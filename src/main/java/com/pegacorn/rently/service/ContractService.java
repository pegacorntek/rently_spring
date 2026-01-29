package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.contract.ContractDto;
import com.pegacorn.rently.dto.contract.CreateContractRequest;
import com.pegacorn.rently.dto.contract.UpdateContractRequest;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ContractService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final RoomTenantRepository roomTenantRepository;
    private final ContractTemplateRepository contractTemplateRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final ServiceFeeRepository serviceFeeRepository;
    private final ContractServiceFeeRepository contractServiceFeeRepository;
    private final HouseSharedAmenityRepository houseSharedAmenityRepository;
    private final AmenityRepository amenityRepository;
    private final ContractSnapshotRepository contractSnapshotRepository;
    private final ActivityLogService activityLogService;
    private final SmsService smsService;

    @Value("${app.base-url:http://localhost:5173}")
    private String appBaseUrl;

    private static final int MAX_SNAPSHOTS_PER_CONTRACT = 20;

    public List<ContractDto> getAllByLandlord(String landlordId, String houseId, String status) {
        List<Contract> contracts;

        if (houseId != null && status != null) {
            contracts = contractRepository.findByHouseIdAndStatus(houseId, Contract.ContractStatus.valueOf(status));
        } else if (houseId != null) {
            contracts = contractRepository.findByHouseId(houseId);
        } else if (status != null) {
            contracts = contractRepository.findByLandlordIdAndStatus(landlordId,
                    Contract.ContractStatus.valueOf(status));
        } else {
            contracts = contractRepository.findByLandlordId(landlordId);
        }

        return contracts.stream()
                .map(this::enrichContract)
                .map(ContractDto::fromEntity)
                .toList();
    }

    public ContractDto getById(String id, String landlordId) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return ContractDto.fromEntity(enrichContract(contract));
    }

    @Transactional
    public ContractDto create(CreateContractRequest request, String landlordId) {
        Room room = roomRepository.findById(request.roomId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Check for active contract on this room
        if (contractRepository.existsByRoomIdAndStatus(request.roomId(), Contract.ContractStatus.ACTIVE)) {
            throw ApiException.conflict(MessageConstant.ROOM_HAS_ACTIVE_CONTRACT);
        }

        // Also check for draft contract on this room
        if (contractRepository.existsByRoomIdAndStatus(request.roomId(), Contract.ContractStatus.DRAFT)) {
            throw ApiException.conflict("Phòng này đã có hợp đồng nháp. Vui lòng xóa hoặc kích hoạt hợp đồng đó trước.");
        }

        // Validate tenants list
        if (request.tenants() == null || request.tenants().isEmpty()) {
            throw ApiException.badRequest("Phải có ít nhất một người thuê");
        }

        // Find primary tenant
        var primaryTenantRequest = request.tenants().stream()
                .filter(CreateContractRequest.TenantRequest::isPrimary)
                .findFirst()
                .orElseThrow(() -> ApiException.badRequest("Phải có một người thuê chính"));

        // Process all tenants - create new or get existing
        User primaryTenant = null;
        java.util.List<User> allTenants = new java.util.ArrayList<>();
        // Track new tenants for SMS notification (User -> password/idNumber)
        java.util.Map<User, String> newTenants = new java.util.HashMap<>();

        for (var tenantReq : request.tenants()) {
            User tenant;
            if (tenantReq.isExisting()) {
                // Existing tenant - find by phone
                tenant = userRepository.findByPhone(tenantReq.phone())
                        .orElseThrow(() -> ApiException.notFound("Không tìm thấy người thuê với SĐT: " + tenantReq.phone()));
                // Update info if provided
                updateTenantInfo(tenant, tenantReq);
            } else {
                // Check if phone already exists
                if (userRepository.existsByPhone(tenantReq.phone())) {
                    throw ApiException.conflict("SĐT " + tenantReq.phone() + " đã tồn tại trong hệ thống");
                }
                // Create new tenant
                tenant = createNewTenant(tenantReq);
                // Track for SMS notification (password is idNumber)
                if (tenantReq.idNumber() != null && !tenantReq.idNumber().isBlank()) {
                    newTenants.put(tenant, tenantReq.idNumber());
                }
            }

            // Ensure user has TENANT role
            ensureTenantRole(tenant);

            allTenants.add(tenant);
            if (tenantReq.isPrimary()) {
                primaryTenant = tenant;
            }
        }

        // Update Landlord info if provided
        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.LANDLORD_NOT_FOUND));
        boolean landlordUpdated = false;
        if (request.landlordAddress() != null && !request.landlordAddress().isBlank()) {
            landlord.setPlaceOfResidence(request.landlordAddress());
            landlordUpdated = true;
        }
        if (request.landlordIdNumber() != null && !request.landlordIdNumber().isBlank()) {
            landlord.setIdNumber(request.landlordIdNumber());
            landlordUpdated = true;
        }
        if (request.landlordIdIssueDate() != null && !request.landlordIdIssueDate().isBlank()) {
            landlord.setIdIssueDate(LocalDate.parse(request.landlordIdIssueDate()));
            landlordUpdated = true;
        }
        if (request.landlordIdIssuePlace() != null && !request.landlordIdIssuePlace().isBlank()) {
            landlord.setIdIssuePlace(request.landlordIdIssuePlace());
            landlordUpdated = true;
        }
        if (landlordUpdated) {
            landlord.setUpdatedAt(LocalDateTime.now());
            userRepository.save(landlord);
        }

        // Calculate end date based on duration
        LocalDate startDate = LocalDate.parse(request.startDate());
        LocalDate endDate = request.durationUnit() == Contract.DurationUnit.MONTH
                ? startDate.plusMonths(request.duration())
                : startDate.plusYears(request.duration());

        // Calculate deposit amount
        BigDecimal depositAmount = request.monthlyRent()
                .multiply(BigDecimal.valueOf(request.depositMonths()));

        Contract contract = Contract.builder()
                .id(UUID.randomUUID().toString())
                .roomId(request.roomId())
                .landlordId(landlordId)
                .tenantId(primaryTenant.getId())
                // Duration
                .duration(request.duration())
                .durationUnit(request.durationUnit())
                .startDate(startDate)
                .endDate(endDate)
                // Payment
                .paymentPeriod(request.paymentPeriod())
                .paymentDueDay(request.paymentDueDay())
                .monthlyRent(request.monthlyRent())
                .depositMonths(request.depositMonths())
                .depositAmount(depositAmount)
                .depositPaid(false)
                // Template
                .templateId(request.templateId())
                .customTerms(request.customTerms())
                // Status
                .status(Contract.ContractStatus.DRAFT)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        contractRepository.save(contract);

        // Add all tenants to RoomTenant table
        for (int i = 0; i < allTenants.size(); i++) {
            User t = allTenants.get(i);
            boolean isPrimary = t.getId().equals(primaryTenant.getId());

            // Check if already exists in room
            if (!roomTenantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(room.getId(), t.getId())) {
                RoomTenant roomTenant = RoomTenant.builder()
                        .id(UUID.randomUUID().toString())
                        .roomId(room.getId())
                        .userId(t.getId())
                        .isPrimary(isPrimary)
                        .joinedAt(LocalDateTime.now())
                        .build();
                roomTenantRepository.save(roomTenant);
            }
        }

        // Save contract service fees if provided
        if (request.serviceFees() != null && !request.serviceFees().isEmpty()) {
            // Use the count of tenants we just added
            final int tenantCount = allTenants.size();

            List<ContractServiceFee> contractFees = request.serviceFees().stream()
                    .map(fee -> {
                        // Fetch the original service fee to get name, feeType, unit
                        ServiceFee originalFee = serviceFeeRepository.findById(fee.serviceFeeId())
                                .orElseThrow(() -> ApiException
                                        .notFound(MessageConstant.SERVICE_FEE_NOT_FOUND + ": " + fee.serviceFeeId()));

                        // fee.amount() is the unit rate (per person for PER_PERSON, total for FIXED)
                        BigDecimal unitRate = fee.amount();
                        BigDecimal totalAmount = unitRate;

                        // For PER_PERSON fees, calculate total = unitRate × tenantCount
                        if (originalFee.getFeeType() == ServiceFee.FeeType.PER_PERSON) {
                            totalAmount = unitRate.multiply(BigDecimal.valueOf(tenantCount));
                        }

                        return ContractServiceFee.builder()
                                .id(UUID.randomUUID().toString())
                                .contractId(contract.getId())
                                .serviceFeeId(fee.serviceFeeId())
                                .name(originalFee.getName())
                                .feeType(originalFee.getFeeType())
                                .unitRate(unitRate)
                                .amount(totalAmount)
                                .unit(originalFee.getUnit())
                                .createdAt(LocalDateTime.now())
                                .build();
                    })
                    .collect(Collectors.toList());
            contractServiceFeeRepository.saveAll(contractFees);
        }

        // Log activity
        activityLogService.logContractCreated(landlordId, contract.getId(), room.getCode(), primaryTenant.getFullName());

        // Send welcome SMS to newly created tenants
        String loginUrl = appBaseUrl + "/login";
        for (var entry : newTenants.entrySet()) {
            User tenant = entry.getKey();
            String password = entry.getValue();
            try {
                smsService.sendTenantWelcome(
                        tenant.getPhone(),
                        tenant.getFullName(),
                        room.getCode(),
                        house.getName(),
                        loginUrl,
                        password
                );
            } catch (Exception e) {
                log.warn("Failed to send welcome SMS to tenant {}: {}", tenant.getPhone(), e.getMessage());
            }
        }

        return ContractDto.fromEntity(enrichContract(contract));
    }

    // Helper methods for tenant handling
    private void updateTenantInfo(User tenant, CreateContractRequest.TenantRequest req) {
        boolean updated = false;
        if (req.fullName() != null && !req.fullName().isBlank()) {
            tenant.setFullName(req.fullName());
            updated = true;
        }
        if (req.idNumber() != null && !req.idNumber().isBlank()) {
            tenant.setIdNumber(req.idNumber());
            updated = true;
        }
        if (req.idIssueDate() != null && !req.idIssueDate().isBlank()) {
            tenant.setIdIssueDate(LocalDate.parse(req.idIssueDate()));
            updated = true;
        }
        if (req.idIssuePlace() != null && !req.idIssuePlace().isBlank()) {
            tenant.setIdIssuePlace(req.idIssuePlace());
            updated = true;
        }
        if (req.dateOfBirth() != null && !req.dateOfBirth().isBlank()) {
            tenant.setDateOfBirth(LocalDate.parse(req.dateOfBirth()));
            updated = true;
        }
        if (req.placeOfOrigin() != null && !req.placeOfOrigin().isBlank()) {
            tenant.setPlaceOfOrigin(req.placeOfOrigin());
            updated = true;
        }
        if (req.gender() != null && !req.gender().isBlank()) {
            tenant.setGender(User.Gender.valueOf(req.gender().toLowerCase()));
            updated = true;
        }
        if (updated) {
            tenant.setUpdatedAt(LocalDateTime.now());
            userRepository.save(tenant);
        }
    }

    private User createNewTenant(CreateContractRequest.TenantRequest req) {
        User tenant = User.builder()
                .id(UUID.randomUUID().toString())
                .phone(req.phone())
                .fullName(req.fullName())
                .idNumber(req.idNumber())
                .status(User.UserStatus.ACTIVE)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        if (req.idIssueDate() != null && !req.idIssueDate().isBlank()) {
            tenant.setIdIssueDate(LocalDate.parse(req.idIssueDate()));
        }
        if (req.idIssuePlace() != null && !req.idIssuePlace().isBlank()) {
            tenant.setIdIssuePlace(req.idIssuePlace());
        }
        if (req.dateOfBirth() != null && !req.dateOfBirth().isBlank()) {
            tenant.setDateOfBirth(LocalDate.parse(req.dateOfBirth()));
        }
        if (req.placeOfOrigin() != null && !req.placeOfOrigin().isBlank()) {
            tenant.setPlaceOfOrigin(req.placeOfOrigin());
        }
        if (req.gender() != null && !req.gender().isBlank()) {
            tenant.setGender(User.Gender.valueOf(req.gender().toLowerCase()));
        }

        return userRepository.save(tenant);
    }

    private void ensureTenantRole(User tenant) {
        if (!userRoleRepository.existsByUserIdAndRole(tenant.getId(), User.Role.TENANT)) {
            UserRole role = UserRole.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(tenant.getId())
                    .role(User.Role.TENANT)
                    .createdAt(LocalDateTime.now())
                    .build();
            userRoleRepository.save(role);
        }
    }

    @Transactional
    public ContractDto update(String id, UpdateContractRequest request, String landlordId) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (contract.getStatus() == Contract.ContractStatus.ENDED) {
            throw ApiException.badRequest(MessageConstant.CANNOT_UPDATE_ENDED_CONTRACT);
        }

        if (request.endDate() != null) {
            contract.setEndDate(LocalDate.parse(request.endDate()));
        }
        if (request.monthlyRent() != null) {
            contract.setMonthlyRent(request.monthlyRent());
            // Sync room price with contract if contract is ACTIVE
            if (contract.getStatus() == Contract.ContractStatus.ACTIVE) {
                Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
                if (room != null) {
                    room.setBaseRent(request.monthlyRent());
                    room.setUpdatedAt(LocalDateTime.now());
                    roomRepository.save(room);
                }
            }
        }

        // Template fields can only be edited for DRAFT contracts
        if (contract.getStatus() == Contract.ContractStatus.DRAFT) {
            if (request.templateId() != null) {
                contract.setTemplateId(request.templateId());
            }
            if (request.customTerms() != null) {
                contract.setCustomTerms(request.customTerms());
            }
        }

        contract.setUpdatedAt(LocalDateTime.now());

        contractRepository.save(contract);
        return ContractDto.fromEntity(enrichContract(contract));
    }

    @Transactional
    public ContractDto activate(String id, String landlordId) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (contract.getStatus() != Contract.ContractStatus.DRAFT) {
            throw ApiException.badRequest(MessageConstant.ONLY_DRAFT_CAN_ACTIVATE);
        }

        // Load contract service fees before rendering
        contract.setServiceFees(contractServiceFeeRepository.findByContractId(contract.getId()));

        // Snapshot the rendered content before activating (freeze template)
        String renderedContent = renderContractContent(contract);
        contract.setContentSnapshot(renderedContent);

        contract.setStatus(Contract.ContractStatus.ACTIVE);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Update room status and sync price with contract
        Room room = roomRepository.findById(contract.getRoomId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));
        room.setStatus(Room.RoomStatus.RENTED);
        room.setBaseRent(contract.getMonthlyRent()); // Sync room price with contract
        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        // Add tenant to room if not already added
        if (!roomTenantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(contract.getRoomId(),
                contract.getTenantId())) {
            RoomTenant roomTenant = RoomTenant.builder()
                    .id(UUID.randomUUID().toString())
                    .roomId(contract.getRoomId())
                    .userId(contract.getTenantId())
                    .isPrimary(true)
                    .joinedAt(LocalDateTime.now())
                    .build();
            roomTenantRepository.save(roomTenant);
        }

        // Log activity
        Contract enriched = enrichContract(contract);
        String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "N/A";
        String tenantName = enriched.getTenant() != null ? enriched.getTenant().getFullName() : "N/A";
        activityLogService.logContractSigned(landlordId, contract.getId(), roomCode, tenantName);

        return ContractDto.fromEntity(enriched);
    }

    /**
     * Refresh the content snapshot for an active contract.
     * Use this when tenants are added/removed after activation.
     */
    @Transactional
    public ContractDto refreshSnapshot(String id, String landlordId) {
        return refreshSnapshot(id, landlordId, null);
    }

    /**
     * Refresh the content snapshot for an active contract with a custom change
     * note.
     * Use this when tenants are added/removed after activation.
     */
    @Transactional
    public ContractDto refreshSnapshot(String id, String landlordId, String changeNote) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            throw ApiException.badRequest(MessageConstant.ONLY_ACTIVE_CAN_REFRESH);
        }

        // Save current snapshot to history before updating
        if (contract.getContentSnapshot() != null) {
            String note = changeNote != null ? changeNote : "Cập nhật văn bản hợp đồng";
            saveSnapshot(contract, note, landlordId);
        }

        // Recalculate PER_PERSON fees based on current tenant count
        recalculatePerPersonFees(contract);

        // Load updated service fees for rendering
        contract.setServiceFees(contractServiceFeeRepository.findByContractId(contract.getId()));

        // Re-render and save new snapshot
        String renderedContent = renderContractContent(contract);
        contract.setContentSnapshot(renderedContent);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);

        return ContractDto.fromEntity(enrichContract(contract));
    }

    /**
     * Save a snapshot to history and enforce max limit
     */
    private void saveSnapshot(Contract contract, String changeNote, String createdBy) {
        // Create snapshot from current content
        ContractSnapshot snapshot = ContractSnapshot.builder()
                .id(UUID.randomUUID().toString())
                .contractId(contract.getId())
                .content(contract.getContentSnapshot())
                .changeNote(changeNote)
                .createdBy(createdBy)
                .createdAt(LocalDateTime.now())
                .build();
        contractSnapshotRepository.save(snapshot);

        // Enforce max snapshot limit
        long count = contractSnapshotRepository.countByContractId(contract.getId());
        if (count > MAX_SNAPSHOTS_PER_CONTRACT) {
            List<ContractSnapshot> oldestSnapshots = contractSnapshotRepository
                    .findByContractIdOrderByCreatedAtAsc(contract.getId());
            int toDelete = (int) (count - MAX_SNAPSHOTS_PER_CONTRACT);
            for (int i = 0; i < toDelete && i < oldestSnapshots.size(); i++) {
                contractSnapshotRepository.delete(oldestSnapshots.get(i));
            }
        }
    }

    /**
     * Recalculate PER_PERSON fees based on current tenant count in the room.
     * Uses the stored unitRate (custom rate set at contract creation).
     */
    private void recalculatePerPersonFees(Contract contract) {
        // Get current tenant count for the room
        List<RoomTenant> tenants = roomTenantRepository.findActiveByRoomId(contract.getRoomId());
        int tenantCount = Math.max(tenants.size(), 1); // Minimum 1 person to avoid zero fees

        // Get all service fees for this contract
        List<ContractServiceFee> contractFees = contractServiceFeeRepository.findByContractId(contract.getId());

        for (ContractServiceFee contractFee : contractFees) {
            if (contractFee.getFeeType() == ServiceFee.FeeType.PER_PERSON) {
                // Use the stored unitRate (custom rate for this contract)
                BigDecimal unitRate = contractFee.getUnitRate();

                // Fallback to original ServiceFee if unitRate is not set (legacy data)
                if (unitRate == null && contractFee.getServiceFeeId() != null) {
                    ServiceFee originalFee = serviceFeeRepository.findById(contractFee.getServiceFeeId()).orElse(null);
                    if (originalFee != null) {
                        unitRate = originalFee.getAmount();
                        // Save the unitRate for future recalculations
                        contractFee.setUnitRate(unitRate);
                    }
                }

                if (unitRate != null) {
                    // Calculate new amount: unitRate × tenantCount
                    BigDecimal newAmount = unitRate.multiply(BigDecimal.valueOf(tenantCount));
                    contractFee.setAmount(newAmount);
                    contractServiceFeeRepository.save(contractFee);
                }
            }
        }
    }

    /**
     * Get snapshot history for landlord (all snapshots)
     */
    public List<ContractSnapshot> getSnapshotHistory(String contractId, String landlordId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return contractSnapshotRepository.findByContractIdOrderByCreatedAtDesc(contractId);
    }

    /**
     * Get snapshot history for tenant (only from their start date)
     */
    public List<ContractSnapshot> getSnapshotHistoryForTenant(String contractId, String tenantId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Only show snapshots from contract start date
        LocalDateTime fromDate = contract.getStartDate().atStartOfDay();
        return contractSnapshotRepository.findByContractIdFromDate(contractId, fromDate);
    }

    @Transactional
    public ContractDto end(String id, String landlordId, boolean removeAllTenants) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // If DRAFT, perform hard delete
        if (contract.getStatus() == Contract.ContractStatus.DRAFT) {
            contractServiceFeeRepository.deleteByContractId(id);
            contractRepository.delete(contract);
            return null;
        }

        if (contract.getStatus() != Contract.ContractStatus.ACTIVE) {
            throw ApiException.badRequest(MessageConstant.ONLY_ACTIVE_OR_DRAFT_CAN_END);
        }

        contract.setStatus(Contract.ContractStatus.ENDED);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Update room status
        Room room = roomRepository.findById(contract.getRoomId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        if (removeAllTenants) {
            // Remove all tenants from room
            roomTenantRepository.markAllAsLeft(contract.getRoomId(), LocalDateTime.now());
            room.setStatus(Room.RoomStatus.EMPTY);
        } else {
            // Remove only the contract tenant
            roomTenantRepository.markAsLeft(contract.getRoomId(), contract.getTenantId(), LocalDateTime.now());
            // Check if room has remaining tenants
            List<RoomTenant> remainingTenants = roomTenantRepository.findActiveByRoomId(contract.getRoomId());
            if (remainingTenants.isEmpty()) {
                room.setStatus(Room.RoomStatus.EMPTY);
            }
        }

        room.setUpdatedAt(LocalDateTime.now());
        roomRepository.save(room);

        // Log activity
        Contract enriched = enrichContract(contract);
        String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "N/A";
        String tenantName = enriched.getTenant() != null ? enriched.getTenant().getFullName() : "N/A";
        activityLogService.logContractEnded(landlordId, contract.getId(), roomCode, tenantName);

        return ContractDto.fromEntity(enriched);
    }

    /**
     * Delete a DRAFT contract permanently.
     * Only DRAFT contracts can be deleted.
     */
    @Transactional
    public void delete(String id, String landlordId) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (contract.getStatus() != Contract.ContractStatus.DRAFT) {
            throw ApiException.badRequest(MessageConstant.ONLY_DRAFT_CAN_DELETE);
        }

        // Delete related service fees first
        contractServiceFeeRepository.deleteByContractId(id);

        // Delete the contract
        contractRepository.delete(contract);
    }

    @Transactional
    public ContractDto confirmDeposit(String id, String landlordId) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (contract.isDepositPaid()) {
            throw ApiException.badRequest(MessageConstant.DEPOSIT_ALREADY_CONFIRMED);
        }

        contract.setDepositPaid(true);
        contract.setUpdatedAt(LocalDateTime.now());
        contractRepository.save(contract);

        // Log activity
        Contract enriched = enrichContract(contract);
        String roomCode = enriched.getRoom() != null ? enriched.getRoom().getCode() : "N/A";
        String tenantName = enriched.getTenant() != null ? enriched.getTenant().getFullName() : "N/A";
        activityLogService.logDepositConfirmed(landlordId, contract.getId(), roomCode, tenantName,
                contract.getDepositAmount().toString());

        return ContractDto.fromEntity(enriched);
    }

    public List<ContractDto> getMyContracts(String tenantId) {
        List<Contract> contracts = contractRepository.findByTenantId(tenantId);
        return contracts.stream()
                .map(this::enrichContract)
                .map(ContractDto::fromEntity)
                .toList();
    }

    public List<ContractDto> getDeposits(String landlordId, String houseId) {
        List<Contract> contracts;
        if (houseId != null) {
            contracts = contractRepository.findByHouseId(houseId);
        } else {
            contracts = contractRepository.findByLandlordId(landlordId);
        }
        return contracts.stream()
                .filter(c -> c.getStatus() != Contract.ContractStatus.ENDED)
                .map(this::enrichContract)
                .map(ContractDto::fromEntity)
                .toList();
    }

    private Contract enrichContract(Contract contract) {
        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
        if (room != null) {
            House house = houseRepository.findById(room.getHouseId()).orElse(null);
            contract.setRoom(Contract.RoomInfo.builder()
                    .id(room.getId())
                    .code(room.getCode())
                    .houseName(house != null ? house.getName() : null)
                    .houseId(room.getHouseId())
                    .build());
        }

        User tenant = userRepository.findById(contract.getTenantId()).orElse(null);
        if (tenant != null) {
            contract.setTenant(Contract.TenantInfo.builder()
                    .id(tenant.getId())
                    .fullName(tenant.getFullName())
                    .phone(tenant.getPhone())
                    .build());
        }

        List<ContractServiceFee> serviceFees = contractServiceFeeRepository.findByContractId(contract.getId());
        contract.setServiceFees(serviceFees);

        return contract;
    }

    /**
     * Render the contract document with all placeholders filled in.
     * For ACTIVE/ENDED contracts, returns the frozen snapshot.
     * For DRAFT contracts, renders from template (allows seeing template updates).
     */
    public String renderContent(String id, String landlordId) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getLandlordId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // For activated/ended contracts, use the frozen snapshot
        if (contract.getStatus() != Contract.ContractStatus.DRAFT && contract.getContentSnapshot() != null) {
            return contract.getContentSnapshot();
        }

        // Load contract service fees before rendering
        contract.setServiceFees(contractServiceFeeRepository.findByContractId(contract.getId()));

        // For draft contracts (or old contracts without snapshot), render from template
        return renderContractContent(contract);
    }

    /**
     * Render the contract document for tenant view.
     * Only allows access to ACTIVE/ENDED contracts (not DRAFT).
     */
    public String renderContentForTenant(String id, String tenantId) {
        Contract contract = contractRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.CONTRACT_NOT_FOUND));

        if (!contract.getTenantId().equals(tenantId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Tenants can only view active or ended contracts
        if (contract.getStatus() == Contract.ContractStatus.DRAFT) {
            throw ApiException.forbidden(MessageConstant.CONTRACT_NOT_ACTIVE);
        }

        // Return the frozen snapshot
        if (contract.getContentSnapshot() != null) {
            return contract.getContentSnapshot();
        }

        // Load contract service fees before rendering (fallback for old contracts)
        contract.setServiceFees(contractServiceFeeRepository.findByContractId(contract.getId()));

        // Fallback for old contracts without snapshot
        return renderContractContent(contract);
    }

    /**
     * Internal method to render contract content from template
     */
    private String renderContractContent(Contract contract) {
        // Get template content
        String content;
        if (contract.getTemplateId() != null) {
            ContractTemplate template = contractTemplateRepository.findById(contract.getTemplateId())
                    .orElse(null);
            content = template != null ? template.getContent() : getDefaultTemplate();
        } else {
            content = getDefaultTemplate();
        }

        // Build placeholder values
        Map<String, String> placeholders = buildPlaceholderValues(contract);

        // Sort by key length (longest first) to avoid corrupting longer placeholders
        // e.g., replace "ngay_cap_cccd_chu_nha" before "cccd_chu_nha"
        List<Map.Entry<String, String>> sortedEntries = placeholders.entrySet().stream()
                .sorted((a, b) -> Integer.compare(b.getKey().length(), a.getKey().length()))
                .toList();

        // Replace placeholders
        for (var entry : sortedEntries) {
            String key = entry.getKey();
            String value = entry.getValue() != null ? entry.getValue() : "";
            String safeValue = Matcher.quoteReplacement(value);

            // Replace span placeholder format (handles TipTap placeholder nodes)
            Pattern spanPattern = Pattern.compile(
                    "<span[^>]*data-placeholder-key=\"\\{\\{" + Pattern.quote(key) + "}}\"[^>]*>.*?</span>",
                    Pattern.DOTALL);
            content = spanPattern.matcher(content).replaceAll(safeValue);

            // Replace {{key}} format
            content = content.replace("{{" + key + "}}", value);
        }

        return content;
    }

    private Map<String, String> buildPlaceholderValues(Contract contract) {
        Map<String, String> values = new HashMap<>();
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        NumberFormat currencyFormatter = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));

        // Get related entities
        Room room = roomRepository.findById(contract.getRoomId()).orElse(null);
        House house = room != null ? houseRepository.findById(room.getHouseId()).orElse(null) : null;
        User landlord = userRepository.findById(contract.getLandlordId()).orElse(null);
        User tenant = userRepository.findById(contract.getTenantId()).orElse(null);

        // Landlord info
        if (landlord != null) {
            String placeOfResident = landlord.getPlaceOfResidence();
            String placeOfOrigin = landlord.getPlaceOfOrigin();

            values.put("ten_chu_nha", landlord.getFullName());
            values.put("sdt_chu_nha", landlord.getPhone());
            values.put("cccd_chu_nha", landlord.getIdNumber() != null ? landlord.getIdNumber() : "___________");
            values.put("ngay_cap_cccd_chu_nha",
                    landlord.getIdIssueDate() != null ? landlord.getIdIssueDate().format(dateFormatter)
                            : "___________");
            values.put("noi_cap_cccd_chu_nha",
                    landlord.getIdIssuePlace() != null ? landlord.getIdIssuePlace() : "___________");
            values.put("dia_chi_chu_nha", placeOfResident != null ? placeOfResident
                    : (placeOfOrigin != null ? placeOfOrigin : "___________"));
        }

        // Tenant info
        if (tenant != null) {
            values.put("ten_nguoi_thue", tenant.getFullName());
            values.put("sdt_nguoi_thue", tenant.getPhone());
            values.put("cccd_nguoi_thue", tenant.getIdNumber() != null ? tenant.getIdNumber() : "___________");
            values.put("ngay_cap_cccd_nguoi_thue",
                    tenant.getIdIssueDate() != null ? tenant.getIdIssueDate().format(dateFormatter) : "___________");
            values.put("noi_cap_cccd_nguoi_thue",
                    tenant.getIdIssuePlace() != null ? tenant.getIdIssuePlace() : "___________");
            values.put("ngay_sinh_nguoi_thue",
                    tenant.getDateOfBirth() != null ? tenant.getDateOfBirth().format(dateFormatter) : "___________");
            values.put("que_quan_nguoi_thue",
                    tenant.getPlaceOfOrigin() != null ? tenant.getPlaceOfOrigin() : "___________");
        }

        // Room info
        if (room != null) {
            values.put("ma_phong", room.getCode());
            values.put("tang", String.valueOf(room.getFloor()));
            values.put("dien_tich", room.getAreaM2() != null ? String.valueOf(room.getAreaM2()) : "___");
            values.put("so_nguoi_toi_da", room.getMaxTenants() != null ? String.valueOf(room.getMaxTenants()) : "___");
        }

        // House info
        if (house != null) {
            values.put("ten_nha_tro", house.getName());
            values.put("dia_chi_nha_tro", house.getAddress());
        }

        // Contract info (don't add VNĐ suffix - template already has it)
        values.put("tien_thue_thang", currencyFormatter.format(contract.getMonthlyRent()));
        values.put("tien_coc", currencyFormatter.format(contract.getDepositAmount()));
        values.put("so_thang_coc", String.valueOf(contract.getDepositMonths()));
        values.put("ngay_bat_dau", contract.getStartDate().format(dateFormatter));
        values.put("ngay_ket_thuc", contract.getEndDate().format(dateFormatter));
        values.put("thoi_han", contract.getDuration() + " "
                + (contract.getDurationUnit() == Contract.DurationUnit.MONTH ? "tháng" : "năm"));
        values.put("ky_thanh_toan", getPaymentPeriodLabel(contract.getPaymentPeriod()));
        values.put("ngay_thanh_toan", String.valueOf(contract.getPaymentDueDay()));
        values.put("ngay_hien_tai", LocalDate.now().format(dateFormatter));

        // Build tables
        values.put("danh_sach_nguoi_o_cung", buildCoTenantsTable(contract.getRoomId()));
        values.put("bang_tien_nghi_phong", buildRoomAmenitiesTable(contract.getRoomId()));
        values.put("bang_tien_nghi_chung", buildSharedAmenitiesTable(house != null ? house.getId() : null));
        values.put("bang_phi_dich_vu", buildContractServiceFeesTable(contract.getServiceFees()));
        values.put("bang_phi_phat_sinh", "<p><em>Không có phí phát sinh</em></p>");

        return values;
    }

    private String getPaymentPeriodLabel(Contract.PaymentPeriod period) {
        return switch (period) {
            case MONTHLY -> "Hàng tháng";
            case QUARTERLY -> "Hàng quý (3 tháng)";
            case BIANNUAL -> "Nửa năm (6 tháng)";
            case ANNUAL -> "Hàng năm";
        };
    }

    private String buildCoTenantsTable(String roomId) {
        List<RoomTenant> tenants = roomTenantRepository.findByRoomIdAndLeftAtIsNull(roomId);
        if (tenants.isEmpty() || tenants.size() <= 1) {
            return "<p><em>Không có người ở cùng</em></p>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<table style=\"width:100%; border-collapse:collapse; border:1px solid #ddd;\">");
        html.append(
                "<tr style=\"background:#f5f5f5;\"><th style=\"border:1px solid #ddd; padding:8px;\">STT</th><th style=\"border:1px solid #ddd; padding:8px;\">Họ tên</th><th style=\"border:1px solid #ddd; padding:8px;\">SĐT</th></tr>");

        int stt = 1;
        for (RoomTenant rt : tenants) {
            if (rt.isPrimary())
                continue; // Skip primary tenant
            User user = userRepository.findById(rt.getUserId()).orElse(null);
            if (user != null) {
                html.append("<tr>");
                html.append("<td style=\"border:1px solid #ddd; padding:8px; text-align:center;\">").append(stt++)
                        .append("</td>");
                html.append("<td style=\"border:1px solid #ddd; padding:8px;\">").append(user.getFullName())
                        .append("</td>");
                html.append("<td style=\"border:1px solid #ddd; padding:8px;\">").append(user.getPhone())
                        .append("</td>");
                html.append("</tr>");
            }
        }
        html.append("</table>");
        return html.toString();
    }

    private String buildRoomAmenitiesTable(String roomId) {
        List<RoomAmenity> amenities = roomAmenityRepository.findByRoomId(roomId);
        if (amenities.isEmpty()) {
            return "<p><em>Không có tài sản</em></p>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<table style=\"width:100%; border-collapse:collapse; border:1px solid #ddd;\">");
        html.append(
                "<tr style=\"background:#f5f5f5;\"><th style=\"border:1px solid #ddd; padding:8px;\">STT</th><th style=\"border:1px solid #ddd; padding:8px;\">Tên tài sản</th><th style=\"border:1px solid #ddd; padding:8px;\">Số lượng</th><th style=\"border:1px solid #ddd; padding:8px;\">Ghi chú</th></tr>");

        int stt = 1;
        for (RoomAmenity ra : amenities) {
            html.append("<tr>");
            html.append("<td style=\"border:1px solid #ddd; padding:8px; text-align:center;\">").append(stt++)
                    .append("</td>");
            html.append("<td style=\"border:1px solid #ddd; padding:8px;\">")
                    .append(ra.getAmenity() != null ? ra.getAmenity().getName() : "N/A").append("</td>");
            html.append("<td style=\"border:1px solid #ddd; padding:8px; text-align:center;\">")
                    .append(ra.getQuantity()).append("</td>");
            html.append("<td style=\"border:1px solid #ddd; padding:8px;\">")
                    .append(ra.getNotes() != null ? ra.getNotes() : "").append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String buildSharedAmenitiesTable(String houseId) {
        if (houseId == null) {
            return "<p><em>Không có tiện nghi chung</em></p>";
        }

        List<HouseSharedAmenity> sharedAmenities = houseSharedAmenityRepository.findByHouseId(houseId);
        if (sharedAmenities.isEmpty()) {
            return "<p><em>Không có tiện nghi chung</em></p>";
        }

        StringBuilder html = new StringBuilder();
        html.append("<table style=\"width:100%; border-collapse:collapse; border:1px solid #ddd;\">");
        html.append(
                "<tr style=\"background:#f5f5f5;\"><th style=\"border:1px solid #ddd; padding:8px;\">STT</th><th style=\"border:1px solid #ddd; padding:8px;\">Tiện nghi</th><th style=\"border:1px solid #ddd; padding:8px;\">Loại</th></tr>");

        int stt = 1;
        for (HouseSharedAmenity hsa : sharedAmenities) {
            Amenity amenity = amenityRepository.findById(hsa.getAmenityId()).orElse(null);
            if (amenity != null) {
                html.append("<tr>");
                html.append("<td style=\"border:1px solid #ddd; padding:8px; text-align:center;\">").append(stt++)
                        .append("</td>");
                html.append("<td style=\"border:1px solid #ddd; padding:8px;\">").append(amenity.getName())
                        .append("</td>");
                html.append("<td style=\"border:1px solid #ddd; padding:8px;\">")
                        .append(getAmenityCategoryLabel(amenity.getCategory())).append("</td>");
                html.append("</tr>");
            }
        }
        html.append("</table>");
        return html.toString();
    }

    private String getAmenityCategoryLabel(Amenity.AmenityCategory category) {
        if (category == null)
            return "";
        return switch (category) {
            case FURNITURE -> "Nội thất";
            case APPLIANCE -> "Thiết bị";
            case UTILITY -> "Tiện ích";
            case FACILITY -> "Cơ sở vật chất";
            case OTHER -> "Khác";
        };
    }

    /**
     * Build service fees table from contract's service fees (custom fees saved at contract creation)
     */
    private String buildContractServiceFeesTable(List<ContractServiceFee> contractFees) {
        if (contractFees == null || contractFees.isEmpty()) {
            return "<p><em>Không có phí dịch vụ</em></p>";
        }

        NumberFormat currencyFormatter = NumberFormat.getInstance(Locale.forLanguageTag("vi-VN"));
        StringBuilder html = new StringBuilder();
        html.append("<table style=\"width:100%; border-collapse:collapse; border:1px solid #ddd;\">");
        html.append(
                "<tr style=\"background:#f5f5f5;\"><th style=\"border:1px solid #ddd; padding:8px;\">STT</th><th style=\"border:1px solid #ddd; padding:8px;\">Loại phí</th><th style=\"border:1px solid #ddd; padding:8px;\">Đơn giá</th><th style=\"border:1px solid #ddd; padding:8px;\">Đơn vị</th></tr>");

        int stt = 1;
        for (ContractServiceFee fee : contractFees) {
            html.append("<tr>");
            html.append("<td style=\"border:1px solid #ddd; padding:8px; text-align:center;\">").append(stt++)
                    .append("</td>");
            html.append("<td style=\"border:1px solid #ddd; padding:8px;\">").append(fee.getName()).append("</td>");
            // Use unitRate for display (the per-unit price), not amount (which may be total for PER_PERSON)
            html.append("<td style=\"border:1px solid #ddd; padding:8px; text-align:right;\">")
                    .append(currencyFormatter.format(fee.getUnitRate())).append(" VNĐ</td>");
            html.append("<td style=\"border:1px solid #ddd; padding:8px;\">")
                    .append(fee.getUnit() != null ? fee.getUnit() : "").append("</td>");
            html.append("</tr>");
        }
        html.append("</table>");
        return html.toString();
    }

    private String getDefaultTemplate() {
        return "<h1 style=\"text-align:center;\">HỢP ĐỒNG THUÊ PHÒNG TRỌ</h1><p>Nội dung hợp đồng mặc định...</p>";
    }
}
