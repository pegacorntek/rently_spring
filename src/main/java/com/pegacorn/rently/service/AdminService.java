package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.announcement.AnnouncementDto;
import com.pegacorn.rently.dto.announcement.CreateAnnouncementRequest;
import com.pegacorn.rently.dto.announcement.UpdateAnnouncementRequest;
import com.pegacorn.rently.dto.auth.UserDto;
import com.pegacorn.rently.dto.activity.ActivityLogDto;
import com.pegacorn.rently.dto.notification.SendNotificationRequest;
import com.pegacorn.rently.dto.setting.CreateSettingRequest;
import com.pegacorn.rently.dto.setting.SystemSettingDto;
import com.pegacorn.rently.dto.setting.UpdateSettingRequest;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.entity.Notification;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.*;
import com.pegacorn.rently.specification.ActivityLogSpecification;
import com.pegacorn.rently.specification.UserSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final ActivityLogRepository activityLogRepository;
    private final SystemSettingRepository systemSettingRepository;
    private final AnnouncementRepository announcementRepository;
    private final NotificationService notificationService;

    // Paginated users with filters (database-level filtering)
    public Map<String, Object> getAllUsersPaginated(int page, int size, String status, String role, String search,
            boolean includeDeleted) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Build specification with filters
        Specification<User> spec = (root, query, cb) -> cb.conjunction();

        // Include/exclude deleted users
        if (!includeDeleted) {
            spec = spec.and(UserSpecification.notDeleted());
        }

        // Status filter
        if (status != null && !status.isEmpty()) {
            try {
                User.UserStatus filterStatus = User.UserStatus.valueOf(status.toUpperCase());
                spec = spec.and(UserSpecification.hasStatus(filterStatus));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Role filter (using subquery)
        if (role != null && !role.isEmpty()) {
            try {
                User.Role filterRole = User.Role.valueOf(role.toUpperCase());
                spec = spec.and(UserSpecification.hasRole(filterRole, "userRole"));
            } catch (IllegalArgumentException ignored) {
            }
        }

        // Search filter
        if (search != null && !search.isEmpty()) {
            spec = spec.and(UserSpecification.searchByNameOrPhone(search));
        }

        // Execute query with specifications
        Page<User> userPage = userRepository.findAll(spec, pageRequest);

        List<UserDto> users = userPage.getContent().stream()
                .peek(user -> user.setRoles(userRoleRepository.findRolesByUserId(user.getId())))
                .map(UserDto::fromEntity)
                .toList();

        return createPageResponse(users, userPage.getTotalElements(), page, size);
    }

    // Backward compatible method (excludes deleted by default)
    public Map<String, Object> getAllUsersPaginated(int page, int size, String status, String role, String search) {
        return getAllUsersPaginated(page, size, status, role, search, false);
    }

    public List<UserDto> getAllUsers() {
        return userRepository.findAll().stream()
                .peek(user -> user.setRoles(userRoleRepository.findRolesByUserId(user.getId())))
                .map(UserDto::fromEntity)
                .toList();
    }

    public UserDto getUserById(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));
        user.setRoles(userRoleRepository.findRolesByUserId(user.getId()));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto lockUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));
        List<User.Role> roles = userRoleRepository.findRolesByUserId(id);
        if (roles.contains(User.Role.SYSTEM_ADMIN)) {
            throw ApiException.forbidden(MessageConstant.CANNOT_LOCK_ADMIN);
        }
        user.setStatus(User.UserStatus.LOCKED);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        user.setRoles(roles);
        return UserDto.fromEntity(user);
    }

    @Transactional
    public UserDto unlockUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));
        user.setStatus(User.UserStatus.ACTIVE);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        user.setRoles(userRoleRepository.findRolesByUserId(user.getId()));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));
        List<User.Role> roles = userRoleRepository.findRolesByUserId(id);
        if (roles.contains(User.Role.SYSTEM_ADMIN)) {
            throw ApiException.forbidden(MessageConstant.CANNOT_DELETE_ADMIN);
        }
        // Soft delete - mark as deleted instead of removing
        user.setStatus(User.UserStatus.DELETED);
        user.setDeletedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public UserDto restoreUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));
        if (user.getStatus() != User.UserStatus.DELETED) {
            throw ApiException.badRequest(MessageConstant.USER_NOT_DELETED);
        }
        user.setStatus(User.UserStatus.ACTIVE);
        user.setDeletedAt(null);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        user.setRoles(userRoleRepository.findRolesByUserId(user.getId()));
        return UserDto.fromEntity(user);
    }

    @Transactional
    public void permanentlyDeleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));
        List<User.Role> roles = userRoleRepository.findRolesByUserId(id);
        if (roles.contains(User.Role.SYSTEM_ADMIN)) {
            throw ApiException.forbidden(MessageConstant.CANNOT_DELETE_ADMIN);
        }
        // Hard delete - remove from database
        userRoleRepository.deleteByUserId(id);
        userRepository.delete(user);
    }

    // Paginated houses
    public Map<String, Object> getAllHousesWithOwnersPaginated(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<House> housePage = houseRepository.findAll(pageRequest);
        List<Map<String, Object>> houses = housePage.getContent().stream().map(house -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", house.getId());
            result.put("name", house.getName());
            result.put("address", house.getAddress());
            result.put("status", house.getStatus().name());
            result.put("roomCount", house.getRoomCount());
            result.put("tenantCount", house.getTenantCount());
            result.put("createdAt", house.getCreatedAt());
            userRepository.findById(house.getOwnerId()).ifPresent(owner -> {
                result.put("ownerName", owner.getFullName());
                result.put("ownerPhone", owner.getPhone());
                result.put("ownerId", owner.getId());
            });
            return result;
        }).collect(Collectors.toList());
        return createPageResponse(houses, housePage.getTotalElements(), page, size);
    }

    public List<Map<String, Object>> getAllHousesWithOwners() {
        List<House> houses = houseRepository.findAll();
        return houses.stream().map(house -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", house.getId());
            result.put("name", house.getName());
            result.put("address", house.getAddress());
            result.put("status", house.getStatus().name());
            result.put("roomCount", house.getRoomCount());
            result.put("tenantCount", house.getTenantCount());
            result.put("createdAt", house.getCreatedAt());
            userRepository.findById(house.getOwnerId()).ifPresent(owner -> {
                result.put("ownerName", owner.getFullName());
                result.put("ownerPhone", owner.getPhone());
                result.put("ownerId", owner.getId());
            });
            return result;
        }).collect(Collectors.toList());
    }

    // Paginated rooms
    public Map<String, Object> getAllRoomsWithDetailsPaginated(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Room> roomPage = roomRepository.findAll(pageRequest);
        Map<String, House> houseMap = houseRepository.findAll().stream()
                .collect(Collectors.toMap(House::getId, h -> h));
        Map<String, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> rooms = roomPage.getContent().stream().map(room -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", room.getId());
            result.put("name", room.getCode());
            result.put("status", room.getStatus().name());
            result.put("basePrice", room.getBaseRent());
            result.put("area", room.getAreaM2());
            result.put("floor", room.getFloor());
            result.put("createdAt", room.getCreatedAt());
            House house = houseMap.get(room.getHouseId());
            if (house != null) {
                result.put("houseName", house.getName());
                result.put("houseId", house.getId());
                User owner = userMap.get(house.getOwnerId());
                if (owner != null) {
                    result.put("ownerName", owner.getFullName());
                    result.put("ownerPhone", owner.getPhone());
                }
            }
            return result;
        }).collect(Collectors.toList());
        return createPageResponse(rooms, roomPage.getTotalElements(), page, size);
    }

    public List<Map<String, Object>> getAllRoomsWithDetails() {
        List<Room> rooms = roomRepository.findAll();
        Map<String, House> houseMap = houseRepository.findAll().stream()
                .collect(Collectors.toMap(House::getId, h -> h));
        Map<String, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, u -> u));
        return rooms.stream().map(room -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", room.getId());
            result.put("name", room.getCode());
            result.put("status", room.getStatus().name());
            result.put("basePrice", room.getBaseRent());
            result.put("area", room.getAreaM2());
            result.put("floor", room.getFloor());
            result.put("createdAt", room.getCreatedAt());
            House house = houseMap.get(room.getHouseId());
            if (house != null) {
                result.put("houseName", house.getName());
                result.put("houseId", house.getId());
                User owner = userMap.get(house.getOwnerId());
                if (owner != null) {
                    result.put("ownerName", owner.getFullName());
                    result.put("ownerPhone", owner.getPhone());
                }
            }
            return result;
        }).collect(Collectors.toList());
    }

    // Paginated contracts
    public Map<String, Object> getAllContractsWithDetailsPaginated(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Contract> contractPage = contractRepository.findAll(pageRequest);
        Map<String, Room> roomMap = roomRepository.findAll().stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<String, House> houseMap = houseRepository.findAll().stream()
                .collect(Collectors.toMap(House::getId, h -> h));
        Map<String, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> contracts = contractPage.getContent().stream().map(contract -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", contract.getId());
            result.put("status", contract.getStatus().name());
            result.put("startDate", contract.getStartDate());
            result.put("endDate", contract.getEndDate());
            result.put("monthlyRent", contract.getMonthlyRent());
            result.put("depositAmount", contract.getDepositAmount());
            result.put("depositPaid", contract.isDepositPaid() ? contract.getDepositAmount() : BigDecimal.ZERO);
            result.put("createdAt", contract.getCreatedAt());
            Room room = roomMap.get(contract.getRoomId());
            if (room != null) {
                result.put("roomName", room.getCode());
                result.put("roomId", room.getId());
                House house = houseMap.get(room.getHouseId());
                if (house != null) {
                    result.put("houseName", house.getName());
                    result.put("houseId", house.getId());
                }
            }
            User tenant = userMap.get(contract.getTenantId());
            if (tenant != null) {
                result.put("tenantName", tenant.getFullName());
                result.put("tenantPhone", tenant.getPhone());
            }
            return result;
        }).collect(Collectors.toList());
        return createPageResponse(contracts, contractPage.getTotalElements(), page, size);
    }

    public List<Map<String, Object>> getAllContractsWithDetails() {
        List<Contract> contracts = contractRepository.findAll();
        Map<String, Room> roomMap = roomRepository.findAll().stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<String, House> houseMap = houseRepository.findAll().stream()
                .collect(Collectors.toMap(House::getId, h -> h));
        Map<String, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, u -> u));
        return contracts.stream().map(contract -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", contract.getId());
            result.put("status", contract.getStatus().name());
            result.put("startDate", contract.getStartDate());
            result.put("endDate", contract.getEndDate());
            result.put("monthlyRent", contract.getMonthlyRent());
            result.put("depositAmount", contract.getDepositAmount());
            result.put("depositPaid", contract.isDepositPaid() ? contract.getDepositAmount() : BigDecimal.ZERO);
            result.put("createdAt", contract.getCreatedAt());
            Room room = roomMap.get(contract.getRoomId());
            if (room != null) {
                result.put("roomName", room.getCode());
                result.put("roomId", room.getId());
                House house = houseMap.get(room.getHouseId());
                if (house != null) {
                    result.put("houseName", house.getName());
                    result.put("houseId", house.getId());
                }
            }
            User tenant = userMap.get(contract.getTenantId());
            if (tenant != null) {
                result.put("tenantName", tenant.getFullName());
                result.put("tenantPhone", tenant.getPhone());
            }
            return result;
        }).collect(Collectors.toList());
    }

    // Paginated invoices
    public Map<String, Object> getAllInvoicesWithDetailsPaginated(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Invoice> invoicePage = invoiceRepository.findAll(pageRequest);
        Map<String, Contract> contractMap = contractRepository.findAll().stream()
                .collect(Collectors.toMap(Contract::getId, c -> c));
        Map<String, Room> roomMap = roomRepository.findAll().stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<String, House> houseMap = houseRepository.findAll().stream()
                .collect(Collectors.toMap(House::getId, h -> h));
        Map<String, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, u -> u));
        List<Map<String, Object>> invoices = invoicePage.getContent().stream().map(invoice -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", invoice.getId());
            result.put("invoiceNumber", invoice.getId().substring(0, 8).toUpperCase());
            result.put("status", invoice.getStatus().name());
            result.put("totalAmount", invoice.getTotalAmount());
            result.put("paidAmount", invoice.getPaidAmount());
            result.put("periodMonth", invoice.getPeriodMonth());
            result.put("dueDate", invoice.getDueDate());
            result.put("createdAt", invoice.getCreatedAt());
            Contract contract = contractMap.get(invoice.getContractId());
            if (contract != null) {
                Room room = roomMap.get(contract.getRoomId());
                if (room != null) {
                    result.put("roomName", room.getCode());
                    House house = houseMap.get(room.getHouseId());
                    if (house != null) {
                        result.put("houseName", house.getName());
                    }
                }
            }
            User tenant = userMap.get(invoice.getTenantId());
            if (tenant != null) {
                result.put("tenantName", tenant.getFullName());
                result.put("tenantPhone", tenant.getPhone());
            }
            return result;
        }).collect(Collectors.toList());
        return createPageResponse(invoices, invoicePage.getTotalElements(), page, size);
    }

    public List<Map<String, Object>> getAllInvoicesWithDetails() {
        List<Invoice> invoices = invoiceRepository.findAll();
        Map<String, Contract> contractMap = contractRepository.findAll().stream()
                .collect(Collectors.toMap(Contract::getId, c -> c));
        Map<String, Room> roomMap = roomRepository.findAll().stream().collect(Collectors.toMap(Room::getId, r -> r));
        Map<String, House> houseMap = houseRepository.findAll().stream()
                .collect(Collectors.toMap(House::getId, h -> h));
        Map<String, User> userMap = userRepository.findAll().stream().collect(Collectors.toMap(User::getId, u -> u));
        return invoices.stream().map(invoice -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", invoice.getId());
            result.put("invoiceNumber", invoice.getId().substring(0, 8).toUpperCase());
            result.put("status", invoice.getStatus().name());
            result.put("totalAmount", invoice.getTotalAmount());
            result.put("paidAmount", invoice.getPaidAmount());
            result.put("periodMonth", invoice.getPeriodMonth());
            result.put("dueDate", invoice.getDueDate());
            result.put("createdAt", invoice.getCreatedAt());
            Contract contract = contractMap.get(invoice.getContractId());
            if (contract != null) {
                Room room = roomMap.get(contract.getRoomId());
                if (room != null) {
                    result.put("roomName", room.getCode());
                    House house = houseMap.get(room.getHouseId());
                    if (house != null) {
                        result.put("houseName", house.getName());
                    }
                }
            }
            User tenant = userMap.get(invoice.getTenantId());
            if (tenant != null) {
                result.put("tenantName", tenant.getFullName());
                result.put("tenantPhone", tenant.getPhone());
            }
            return result;
        }).collect(Collectors.toList());
    }

    public Map<String, Object> getFinancialReport(int year, Integer month) {
        Map<String, Object> report = new LinkedHashMap<>();
        LocalDateTime startDate, endDate;
        if (month != null) {
            YearMonth ym = YearMonth.of(year, month);
            startDate = ym.atDay(1).atStartOfDay();
            endDate = ym.atEndOfMonth().atTime(23, 59, 59);
        } else {
            startDate = LocalDate.of(year, 1, 1).atStartOfDay();
            endDate = LocalDate.of(year, 12, 31).atTime(23, 59, 59);
        }
        List<Payment> payments = paymentRepository.findAll().stream()
                .filter(p -> p.getCreatedAt().isAfter(startDate) && p.getCreatedAt().isBefore(endDate)).toList();
        BigDecimal totalRevenue = payments.stream().map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        List<Invoice> invoices = invoiceRepository.findAll().stream()
                .filter(i -> i.getCreatedAt().isAfter(startDate) && i.getCreatedAt().isBefore(endDate)).toList();
        long paidInvoices = invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.PAID).count();
        long unpaidInvoices = invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.SENT).count();
        long overdueInvoices = invoices.stream().filter(i -> i.getStatus() == Invoice.InvoiceStatus.SENT
                && i.getDueDate() != null && i.getDueDate().isBefore(LocalDate.now())).count();
        BigDecimal totalBilled = invoices.stream().map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO,
                BigDecimal::add);
        BigDecimal totalPaid = invoices.stream().map(Invoice::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        report.put("totalRevenue", totalRevenue);
        report.put("totalBilled", totalBilled);
        report.put("totalPaid", totalPaid);
        report.put("totalOutstanding", totalBilled.subtract(totalPaid));
        report.put("paidInvoices", paidInvoices);
        report.put("unpaidInvoices", unpaidInvoices);
        report.put("overdueInvoices", overdueInvoices);
        report.put("totalInvoices", invoices.size());
        report.put("totalPayments", payments.size());
        if (month == null) {
            List<Map<String, Object>> monthlyData = new ArrayList<>();
            for (int m = 1; m <= 12; m++) {
                YearMonth ym = YearMonth.of(year, m);
                LocalDateTime mStart = ym.atDay(1).atStartOfDay();
                LocalDateTime mEnd = ym.atEndOfMonth().atTime(23, 59, 59);
                final int fm = m;
                BigDecimal monthRevenue = payments.stream()
                        .filter(p -> p.getCreatedAt().isAfter(mStart) && p.getCreatedAt().isBefore(mEnd))
                        .map(Payment::getAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                long monthInvoices = invoices.stream()
                        .filter(i -> i.getCreatedAt().isAfter(mStart) && i.getCreatedAt().isBefore(mEnd)).count();
                Map<String, Object> monthData = new LinkedHashMap<>();
                monthData.put("month", fm);
                monthData.put("revenue", monthRevenue);
                monthData.put("invoices", monthInvoices);
                monthlyData.add(monthData);
            }
            report.put("monthlyBreakdown", monthlyData);
        }
        return report;
    }

    // Paginated activity logs with filters (database-level filtering)
    public Map<String, Object> getActivityLogsPaginated(int page, int size, String type, String userId,
            LocalDateTime startDate, LocalDateTime endDate) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

        // Build specification with filters
        Specification<ActivityLog> spec = (root, query, cb) -> cb.conjunction();

        // Type filter
        if (type != null && !type.isEmpty()) {
            spec = spec.and(ActivityLogSpecification.hasTypeString(type));
        }

        // User filter
        if (userId != null && !userId.isEmpty()) {
            spec = spec.and(ActivityLogSpecification.hasUserId(userId));
        }

        // Date range filter
        spec = spec.and(ActivityLogSpecification.createdBetween(startDate, endDate));

        // Execute query with specifications
        Page<ActivityLog> logPage = activityLogRepository.findAll(spec, pageRequest);

        List<ActivityLogDto> logs = logPage.getContent().stream()
                .map(ActivityLogDto::fromEntity)
                .toList();

        return createPageResponse(logs, logPage.getTotalElements(), page, size);
    }

    public List<ActivityLogDto> getActivityLogs(int limit, int offset) {
        return activityLogRepository
                .findAll(PageRequest.of(offset / limit, limit, Sort.by(Sort.Direction.DESC, "createdAt"))).stream()
                .map(ActivityLogDto::fromEntity).toList();
    }

    public long getActivityLogsCount() {
        return activityLogRepository.count();
    }

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new LinkedHashMap<>();
        List<User> allUsers = userRepository.findAll();

        // Total users with breakdown
        stats.put("totalUsers", allUsers.size());
        stats.put("activeUsers", allUsers.stream().filter(u -> u.getStatus() == User.UserStatus.ACTIVE).count());
        stats.put("lockedUsers", allUsers.stream().filter(u -> u.getStatus() == User.UserStatus.LOCKED).count());

        // Count by role
        Map<String, List<User.Role>> userRolesMap = new HashMap<>();
        for (User user : allUsers) {
            List<User.Role> roles = userRoleRepository.findRolesByUserId(user.getId());
            userRolesMap.put(user.getId(), roles);
        }

        long totalLandlords = userRolesMap.values().stream()
                .filter(roles -> roles.contains(User.Role.LANDLORD))
                .count();
        long totalTenants = userRolesMap.values().stream()
                .filter(roles -> roles.contains(User.Role.TENANT))
                .count();

        stats.put("totalLandlords", totalLandlords);
        stats.put("totalTenants", totalTenants);

        // Platform scale
        stats.put("totalHouses", houseRepository.count());
        stats.put("totalRooms", roomRepository.count());

        return stats;
    }

    // Helper method to create paginated response
    private Map<String, Object> createPageResponse(List<?> content, long totalElements, int page, int size) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("content", content);
        response.put("totalElements", totalElements);
        response.put("totalPages", (int) Math.ceil((double) totalElements / size));
        response.put("page", page);
        response.put("size", size);
        response.put("hasNext", (page + 1) * size < totalElements);
        response.put("hasPrevious", page > 0);
        return response;
    }

    // ==================== SETTINGS ====================

    public List<SystemSettingDto> getAllSettings() {
        return systemSettingRepository.findAll().stream()
                .map(SystemSettingDto::fromEntity)
                .toList();
    }

    public SystemSettingDto getSettingByKey(String key) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.SETTING_NOT_FOUND + key));
        return SystemSettingDto.fromEntity(setting);
    }

    @Transactional
    public SystemSettingDto createSetting(CreateSettingRequest request) {
        if (systemSettingRepository.existsByKey(request.key())) {
            throw ApiException.conflict(MessageConstant.SETTING_ALREADY_EXISTS + request.key());
        }

        SystemSetting setting = SystemSetting.builder()
                .id(UUID.randomUUID().toString())
                .key(request.key())
                .value(request.value())
                .description(request.description())
                .valueType(request.valueType() != null ? request.valueType() : "STRING")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        systemSettingRepository.save(setting);
        return SystemSettingDto.fromEntity(setting);
    }

    @Transactional
    public SystemSettingDto updateSetting(String key, UpdateSettingRequest request) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.SETTING_NOT_FOUND + key));

        if (request.value() != null) {
            setting.setValue(request.value());
        }
        if (request.description() != null) {
            setting.setDescription(request.description());
        }
        setting.setUpdatedAt(LocalDateTime.now());

        systemSettingRepository.save(setting);
        return SystemSettingDto.fromEntity(setting);
    }

    @Transactional
    public void deleteSetting(String key) {
        SystemSetting setting = systemSettingRepository.findByKey(key)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.SETTING_NOT_FOUND + key));
        systemSettingRepository.delete(setting);
    }

    @Transactional
    public List<SystemSettingDto> updateBulkSettings(Map<String, String> settings) {
        List<SystemSetting> updatedSettings = settings.entrySet().stream().map(entry -> {
            SystemSetting setting = systemSettingRepository.findByKey(entry.getKey())
                    .orElseGet(() -> SystemSetting.builder()
                            .id(UUID.randomUUID().toString())
                            .key(entry.getKey())
                            .valueType("STRING")
                            .createdAt(LocalDateTime.now())
                            .build());
            setting.setValue(entry.getValue());
            setting.setUpdatedAt(LocalDateTime.now());
            return systemSettingRepository.save(setting);
        }).toList();

        return updatedSettings.stream().map(SystemSettingDto::fromEntity).toList();
    }

    // ==================== ANNOUNCEMENTS ====================

    public Map<String, Object> getAllAnnouncementsPaginated(int page, int size) {
        PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Announcement> announcementPage = announcementRepository.findAll(pageRequest);
        List<AnnouncementDto> announcements = announcementPage.getContent().stream()
                .map(AnnouncementDto::fromEntity)
                .toList();
        return createPageResponse(announcements, announcementPage.getTotalElements(), page, size);
    }

    public AnnouncementDto getAnnouncementById(String id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ANNOUNCEMENT_NOT_FOUND));
        return AnnouncementDto.fromEntity(announcement);
    }

    @Transactional
    public AnnouncementDto createAnnouncement(CreateAnnouncementRequest request, String userId) {
        Announcement announcement = Announcement.builder()
                .id(UUID.randomUUID().toString())
                .title(request.title())
                .content(request.content())
                .type(parseAnnouncementType(request.type()))
                .status(Announcement.AnnouncementStatus.DRAFT)
                .targetAudience(parseTargetAudience(request.targetAudience()))
                .publishAt(request.publishAt())
                .expireAt(request.expireAt())
                .createdBy(userId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        announcementRepository.save(announcement);
        return AnnouncementDto.fromEntity(announcement);
    }

    @Transactional
    public AnnouncementDto updateAnnouncement(String id, UpdateAnnouncementRequest request) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ANNOUNCEMENT_NOT_FOUND));

        if (request.title() != null)
            announcement.setTitle(request.title());
        if (request.content() != null)
            announcement.setContent(request.content());
        if (request.type() != null)
            announcement.setType(parseAnnouncementType(request.type()));
        if (request.status() != null)
            announcement.setStatus(parseAnnouncementStatus(request.status()));
        if (request.targetAudience() != null)
            announcement.setTargetAudience(parseTargetAudience(request.targetAudience()));
        if (request.publishAt() != null)
            announcement.setPublishAt(request.publishAt());
        if (request.expireAt() != null)
            announcement.setExpireAt(request.expireAt());
        announcement.setUpdatedAt(LocalDateTime.now());

        announcementRepository.save(announcement);
        return AnnouncementDto.fromEntity(announcement);
    }

    @Transactional
    public AnnouncementDto publishAnnouncement(String id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ANNOUNCEMENT_NOT_FOUND));

        announcement.setStatus(Announcement.AnnouncementStatus.PUBLISHED);
        if (announcement.getPublishAt() == null) {
            announcement.setPublishAt(LocalDateTime.now());
        }
        announcement.setUpdatedAt(LocalDateTime.now());

        announcementRepository.save(announcement);

        // Send notifications to target users
        sendAnnouncementNotifications(announcement);

        return AnnouncementDto.fromEntity(announcement);
    }

    private void sendAnnouncementNotifications(Announcement announcement) {
        List<String> targetUserIds = getTargetUserIds(announcement.getTargetAudience());

        Map<String, Object> data = new HashMap<>();
        data.put("announcementId", announcement.getId());
        data.put("type", announcement.getType().name());

        for (String userId : targetUserIds) {
            try {
                notificationService.createNotification(
                        userId,
                        Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        announcement.getTitle(),
                        announcement.getContent(),
                        data);
            } catch (Exception e) {
                // Log but continue with other users
            }
        }
    }

    private List<String> getTargetUserIds(Announcement.TargetAudience audience) {
        List<User> users = userRepository.findAll().stream()
                .filter(u -> u.getStatus() == User.UserStatus.ACTIVE)
                .toList();

        if (audience == Announcement.TargetAudience.ALL) {
            return users.stream().map(User::getId).toList();
        }

        User.Role targetRole = audience == Announcement.TargetAudience.LANDLORDS
                ? User.Role.LANDLORD
                : User.Role.TENANT;

        return users.stream()
                .filter(user -> {
                    List<User.Role> roles = userRoleRepository.findRolesByUserId(user.getId());
                    return roles.contains(targetRole);
                })
                .map(User::getId)
                .toList();
    }

    @Transactional
    public AnnouncementDto archiveAnnouncement(String id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ANNOUNCEMENT_NOT_FOUND));

        announcement.setStatus(Announcement.AnnouncementStatus.ARCHIVED);
        announcement.setUpdatedAt(LocalDateTime.now());

        announcementRepository.save(announcement);
        return AnnouncementDto.fromEntity(announcement);
    }

    @Transactional
    public void deleteAnnouncement(String id) {
        Announcement announcement = announcementRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ANNOUNCEMENT_NOT_FOUND));
        announcementRepository.delete(announcement);
    }

    public List<AnnouncementDto> getActiveAnnouncements(String audience) {
        Announcement.TargetAudience targetAudience = parseTargetAudience(audience);
        return announcementRepository.findActiveAnnouncements(LocalDateTime.now(), targetAudience)
                .stream()
                .map(AnnouncementDto::fromEntity)
                .toList();
    }

    // ==================== HELPER METHODS ====================

    private Announcement.AnnouncementType parseAnnouncementType(String type) {
        if (type == null)
            return Announcement.AnnouncementType.INFO;
        try {
            return Announcement.AnnouncementType.valueOf(type.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Announcement.AnnouncementType.INFO;
        }
    }

    private Announcement.AnnouncementStatus parseAnnouncementStatus(String status) {
        if (status == null)
            return Announcement.AnnouncementStatus.DRAFT;
        try {
            return Announcement.AnnouncementStatus.valueOf(status.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Announcement.AnnouncementStatus.DRAFT;
        }
    }

    private Announcement.TargetAudience parseTargetAudience(String audience) {
        if (audience == null)
            return Announcement.TargetAudience.ALL;
        try {
            return Announcement.TargetAudience.valueOf(audience.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Announcement.TargetAudience.ALL;
        }
    }

    // ==================== DIRECT NOTIFICATIONS ====================

    public int sendNotificationToUsers(SendNotificationRequest request) {
        List<String> targetUserIds;

        // If specific userIds provided, use them
        if (request.userIds() != null && !request.userIds().isEmpty()) {
            targetUserIds = request.userIds();
        } else {
            // Use targetAudience to determine users
            Announcement.TargetAudience audience = parseTargetAudience(request.targetAudience());
            targetUserIds = getTargetUserIds(audience);
        }

        int sentCount = 0;
        for (String userId : targetUserIds) {
            try {
                notificationService.createNotification(
                        userId,
                        Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                        request.title(),
                        request.message(),
                        null);
                sentCount++;
            } catch (Exception e) {
                // Log but continue with other users
            }
        }

        return sentCount;
    }

    public void sendNotificationToUser(String userId, String title, String message) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        notificationService.createNotification(
                userId,
                Notification.NotificationType.SYSTEM_ANNOUNCEMENT,
                title,
                message,
                null);
    }
}
