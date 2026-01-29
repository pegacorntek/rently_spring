package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.tenant.HousemateDto;
import com.pegacorn.rently.dto.tenant.TenantListDto;
import com.pegacorn.rently.dto.tenant.UpdateTenantRequest;
import com.pegacorn.rently.entity.House;
import com.pegacorn.rently.entity.Room;
import com.pegacorn.rently.entity.RoomTenant;
import com.pegacorn.rently.entity.User;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.HouseRepository;
import com.pegacorn.rently.repository.RoomRepository;
import com.pegacorn.rently.repository.RoomTenantRepository;
import com.pegacorn.rently.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TenantService {

    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;
    private final RoomTenantRepository roomTenantRepository;
    private final UserRepository userRepository;

    public Page<TenantListDto> getAllTenantsForLandlord(String ownerId, String houseId, String search, Pageable pageable) {
        // Get all tenants first
        List<TenantListDto> allTenants = fetchAllTenants(ownerId);

        // Apply house filter
        if (houseId != null && !houseId.isBlank()) {
            allTenants = allTenants.stream()
                    .filter(t -> houseId.equals(t.house().id()))
                    .toList();
        }

        // Apply search filter
        if (search != null && !search.isBlank()) {
            String searchLower = search.toLowerCase().trim();
            allTenants = allTenants.stream()
                    .filter(t -> matchesSearch(t, searchLower))
                    .toList();
        }

        // Apply pagination
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), allTenants.size());

        if (start > allTenants.size()) {
            return new PageImpl<>(List.of(), pageable, allTenants.size());
        }

        List<TenantListDto> pageContent = allTenants.subList(start, end);
        return new PageImpl<>(pageContent, pageable, allTenants.size());
    }

    private boolean matchesSearch(TenantListDto tenant, String search) {
        return (tenant.fullName() != null && tenant.fullName().toLowerCase().contains(search)) ||
                (tenant.phone() != null && tenant.phone().contains(search)) ||
                (tenant.idNumber() != null && tenant.idNumber().contains(search)) ||
                (tenant.room().code() != null && tenant.room().code().toLowerCase().contains(search)) ||
                (tenant.house().name() != null && tenant.house().name().toLowerCase().contains(search));
    }

    private List<TenantListDto> fetchAllTenants(String ownerId) {
        // Get all houses owned by landlord
        List<House> houses = houseRepository.findByOwnerId(ownerId);
        if (houses.isEmpty()) {
            return List.of();
        }

        // Map house by id for quick lookup
        Map<String, House> houseMap = houses.stream()
                .collect(Collectors.toMap(House::getId, h -> h));

        // Get all rooms in those houses
        List<String> houseIds = houses.stream().map(House::getId).toList();
        List<Room> rooms = roomRepository.findByHouseIdIn(houseIds);
        if (rooms.isEmpty()) {
            return List.of();
        }

        // Map room by id for quick lookup
        Map<String, Room> roomMap = rooms.stream()
                .collect(Collectors.toMap(Room::getId, r -> r));

        // Get all active tenants in those rooms
        List<String> roomIds = rooms.stream().map(Room::getId).toList();
        List<RoomTenant> roomTenants = roomTenantRepository.findActiveByRoomIds(roomIds);
        if (roomTenants.isEmpty()) {
            return List.of();
        }

        // Get all user details
        List<String> userIds = roomTenants.stream().map(RoomTenant::getUserId).distinct().toList();
        Map<String, User> userMap = userRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // Build result
        List<TenantListDto> result = new ArrayList<>();
        for (RoomTenant rt : roomTenants) {
            Room room = roomMap.get(rt.getRoomId());
            if (room == null)
                continue;

            House house = houseMap.get(room.getHouseId());
            if (house == null)
                continue;

            User user = userMap.get(rt.getUserId());
            if (user == null)
                continue;

            result.add(new TenantListDto(
                    rt.getId(),
                    rt.getUserId(),
                    user.getFullName(),
                    user.getPhone(),
                    user.getIdNumber(),
                    user.getIdIssueDate(),
                    user.getIdIssuePlace(),
                    user.getGender() != null ? user.getGender().name() : null,
                    user.getDateOfBirth(),
                    user.getPlaceOfOrigin(),
                    rt.isPrimary(),
                    rt.getJoinedAt(),
                    new TenantListDto.RoomInfo(room.getId(), room.getCode(), room.getFloor()),
                    new TenantListDto.HouseInfo(house.getId(), house.getName(), house.getAddress())));
        }

        return result;
    }

    @Transactional
    public TenantListDto updateTenant(String roomTenantId, String ownerId, UpdateTenantRequest request) {
        // Find the room tenant record
        RoomTenant roomTenant = roomTenantRepository.findById(roomTenantId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TENANT_NOT_FOUND));

        // Verify landlord owns this tenant's room
        Room room = roomRepository.findById(roomTenant.getRoomId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));
        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        // Update user info
        User user = userRepository.findById(roomTenant.getUserId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        user.setFullName(request.fullName());
        if (request.phone() != null)
            user.setPhone(request.phone());
        if (request.idNumber() != null)
            user.setIdNumber(request.idNumber());
        if (request.idIssueDate() != null && !request.idIssueDate().isBlank()) {
            user.setIdIssueDate(LocalDate.parse(request.idIssueDate()));
        }
        if (request.idIssuePlace() != null)
            user.setIdIssuePlace(request.idIssuePlace());
        if (request.gender() != null)
            user.setGender(request.gender());
        if (request.dateOfBirth() != null && !request.dateOfBirth().isBlank()) {
            user.setDateOfBirth(LocalDate.parse(request.dateOfBirth()));
        }
        if (request.placeOfOrigin() != null)
            user.setPlaceOfOrigin(request.placeOfOrigin());

        userRepository.save(user);

        // Return updated tenant info
        return new TenantListDto(
                roomTenant.getId(),
                roomTenant.getUserId(),
                user.getFullName(),
                user.getPhone(),
                user.getIdNumber(),
                user.getIdIssueDate(),
                user.getIdIssuePlace(),
                user.getGender() != null ? user.getGender().name() : null,
                user.getDateOfBirth(),
                user.getPlaceOfOrigin(),
                roomTenant.isPrimary(),
                roomTenant.getJoinedAt(),
                new TenantListDto.RoomInfo(room.getId(), room.getCode(), room.getFloor()),
                new TenantListDto.HouseInfo(house.getId(), house.getName(), house.getAddress()));
    }

    public List<HousemateDto> getMyHousemates(String currentUserId) {
        List<HousemateDto> housemates = new ArrayList<>();

        // Find all rooms where the current user is an active tenant
        List<Room> myRooms = roomRepository.findByTenantUserId(currentUserId);

        if (myRooms.isEmpty()) {
            return housemates;
        }

        // Get unique house IDs
        List<String> houseIds = myRooms.stream()
                .map(Room::getHouseId)
                .distinct()
                .toList();

        // Get all houses
        Map<String, House> housesById = houseRepository.findAllById(houseIds).stream()
                .collect(Collectors.toMap(House::getId, h -> h));

        // Get all rooms in those houses
        List<Room> allRoomsInHouses = roomRepository.findByHouseIdIn(houseIds);
        Map<String, Room> allRoomsById = allRoomsInHouses.stream()
                .collect(Collectors.toMap(Room::getId, r -> r));

        // Get all active tenants in those rooms WITH user details
        List<String> allRoomIds = allRoomsInHouses.stream()
                .map(Room::getId)
                .toList();

        List<RoomTenant> allTenantsInHouses = roomTenantRepository.findActiveWithUserByRoomIds(allRoomIds);

        // Build the housemate list with privacy logic
        for (RoomTenant tenant : allTenantsInHouses) {
            Room room = allRoomsById.get(tenant.getRoomId());
            if (room == null)
                continue;

            House house = housesById.get(room.getHouseId());
            if (house == null)
                continue;

            boolean isCurrentUser = tenant.getUserId().equals(currentUserId);

            // Privacy logic
            String displayName = (isCurrentUser || tenant.isShareFullName()) ? tenant.getFullName() : "Người thuê";
            String displayPhone = (isCurrentUser || tenant.isSharePhone()) ? tenant.getPhone() : null;
            String displayGender = (isCurrentUser || tenant.isShareGender())
                    ? (tenant.getGender() != null ? tenant.getGender().name() : null)
                    : null;
            String displayOrigin = (isCurrentUser || tenant.isShareOrigin()) ? tenant.getPlaceOfOrigin() : null;

            housemates.add(HousemateDto.builder()
                    .fullName(displayName)
                    .phone(displayPhone)
                    .gender(displayGender)
                    .placeOfOrigin(displayOrigin)
                    .roomCode(room.getCode())
                    .houseName(house.getName())
                    .isPrimary(tenant.isPrimary())
                    .isCurrentUser(isCurrentUser)
                    .build());
        }

        return housemates;
    }

    @Transactional
    public void updatePrivacySettings(String userId, com.pegacorn.rently.dto.tenant.UpdatePrivacyRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.USER_NOT_FOUND));

        user.setShareFullName(request.shareFullName());
        user.setSharePhone(request.sharePhone());
        user.setShareOrigin(request.shareOrigin());
        user.setShareGender(request.shareGender());

        userRepository.save(user);
    }

    public boolean isTenantOfLandlord(String phone, String ownerId) {
        return roomTenantRepository.isTenantOfLandlord(phone, ownerId);
    }
}
