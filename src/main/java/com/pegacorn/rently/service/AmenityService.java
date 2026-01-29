package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.amenity.*;
import com.pegacorn.rently.entity.Amenity;
import com.pegacorn.rently.entity.House;
import com.pegacorn.rently.entity.HouseSharedAmenity;
import com.pegacorn.rently.entity.Room;
import com.pegacorn.rently.entity.RoomAmenity;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.AmenityRepository;
import com.pegacorn.rently.repository.HouseRepository;
import com.pegacorn.rently.repository.HouseSharedAmenityRepository;
import com.pegacorn.rently.repository.RoomAmenityRepository;
import com.pegacorn.rently.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AmenityService {

    private final AmenityRepository amenityRepository;
    private final RoomAmenityRepository roomAmenityRepository;
    private final HouseSharedAmenityRepository houseSharedAmenityRepository;
    private final HouseRepository houseRepository;
    private final RoomRepository roomRepository;

    // ============ AMENITY CATALOG ============

    public List<AmenityDto> getAll(String houseId, String ownerId) {
        if (houseId == null || houseId.isEmpty()) {
            throw ApiException.badRequest(MessageConstant.HOUSE_ID_REQUIRED);
        }

        // Validate house ownership
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return amenityRepository.findByHouseId(houseId)
                .stream()
                .map(AmenityDto::from)
                .toList();
    }

    @Transactional
    public AmenityDto create(CreateAmenityRequest request, String ownerId) {
        // Require houseId - all amenities are house-specific
        if (request.houseId() == null || request.houseId().isEmpty()) {
            throw ApiException.badRequest(MessageConstant.HOUSE_ID_REQUIRED);
        }

        // Validate house ownership
        House house = houseRepository.findById(request.houseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        Amenity amenity = Amenity.builder()
                .id(UUID.randomUUID().toString())
                .houseId(request.houseId())
                .name(request.name())
                .category(request.category())
                .icon(request.icon())
                .price(request.price())
                .isCustom(true)
                .createdAt(LocalDateTime.now())
                .build();

        amenityRepository.save(amenity);
        return AmenityDto.from(amenity);
    }

    @Transactional
    public AmenityDto update(String id, UpdateAmenityRequest request, String ownerId) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.AMENITY_NOT_FOUND));

        // Validate house ownership
        House house = houseRepository.findById(amenity.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        if (request.name() != null) {
            amenity.setName(request.name());
        }
        if (request.category() != null) {
            amenity.setCategory(request.category());
        }
        if (request.icon() != null) {
            amenity.setIcon(request.icon());
        }
        if (request.price() != null) {
            amenity.setPrice(request.price());
        }

        amenityRepository.save(amenity);
        return AmenityDto.from(amenity);
    }

    @Transactional
    public void delete(String id, String ownerId) {
        Amenity amenity = amenityRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.AMENITY_NOT_FOUND));

        // Validate house ownership
        House house = houseRepository.findById(amenity.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));
        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        amenityRepository.delete(amenity);
    }

    // ============ ROOM AMENITIES ============

    public List<RoomAmenityDto> getRoomAmenities(String roomId, String ownerId) {
        validateRoomAccess(roomId, ownerId);
        return roomAmenityRepository.findByRoomId(roomId)
                .stream()
                .map(RoomAmenityDto::from)
                .toList();
    }

    @Transactional
    public RoomAmenityDto addRoomAmenity(String roomId, AddRoomAmenityRequest request, String ownerId) {
        validateRoomAccess(roomId, ownerId);

        // Check if amenity exists
        Amenity amenity = amenityRepository.findById(request.amenityId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.AMENITY_NOT_FOUND));

        // Check if already added
        if (roomAmenityRepository.existsByRoomIdAndAmenityId(roomId, request.amenityId())) {
            throw ApiException.conflict(MessageConstant.AMENITY_ALREADY_ADDED);
        }

        RoomAmenity roomAmenity = RoomAmenity.builder()
                .id(UUID.randomUUID().toString())
                .roomId(roomId)
                .amenityId(request.amenityId())
                .quantity(request.quantity() != null ? request.quantity() : 1)
                .notes(request.notes())
                .condition(request.condition() != null
                        ? com.pegacorn.rently.entity.AmenityCondition.valueOf(request.condition())
                        : com.pegacorn.rently.entity.AmenityCondition.NEW)
                .createdAt(LocalDateTime.now())
                .build();

        roomAmenityRepository.save(roomAmenity);

        // Reload with amenity data
        roomAmenity.setAmenity(amenity);
        return RoomAmenityDto.from(roomAmenity);
    }

    @Transactional
    public RoomAmenityDto updateRoomAmenity(String roomId, String amenityId, UpdateRoomAmenityRequest request,
            String ownerId) {
        validateRoomAccess(roomId, ownerId);

        RoomAmenity roomAmenity = roomAmenityRepository.findByRoomIdAndAmenityId(roomId, amenityId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_AMENITY_NOT_FOUND));

        if (request.quantity() != null) {
            roomAmenity.setQuantity(request.quantity());
        }
        if (request.notes() != null) {
            roomAmenity.setNotes(request.notes());
        }
        if (request.condition() != null) {
            roomAmenity.setCondition(com.pegacorn.rently.entity.AmenityCondition.valueOf(request.condition()));
        }

        roomAmenityRepository.save(roomAmenity);
        return RoomAmenityDto.from(roomAmenity);
    }

    @Transactional
    public void removeRoomAmenity(String roomId, String amenityId, String ownerId) {
        validateRoomAccess(roomId, ownerId);
        roomAmenityRepository.deleteByRoomIdAndAmenityId(roomId, amenityId);
    }

    // ============ HOUSE SHARED AMENITIES ============

    public List<HouseSharedAmenityDto> getHouseSharedAmenities(String houseId, String ownerId) {
        validateHouseAccess(houseId, ownerId);

        List<HouseSharedAmenity> sharedAmenities = houseSharedAmenityRepository.findByHouseId(houseId);

        // Enrich with amenity data
        for (HouseSharedAmenity hsa : sharedAmenities) {
            amenityRepository.findById(hsa.getAmenityId()).ifPresent(hsa::setAmenity);
        }

        return sharedAmenities.stream()
                .map(HouseSharedAmenityDto::fromEntity)
                .toList();
    }

    @Transactional
    public HouseSharedAmenityDto addHouseSharedAmenity(String houseId, AddHouseSharedAmenityRequest request,
            String ownerId) {
        validateHouseAccess(houseId, ownerId);

        Amenity amenity = amenityRepository.findById(request.amenityId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.AMENITY_NOT_FOUND));

        // Check if already added
        if (houseSharedAmenityRepository.findByHouseIdAndAmenityId(houseId, request.amenityId()).isPresent()) {
            throw ApiException.conflict(MessageConstant.AMENITY_ALREADY_SHARED);
        }

        HouseSharedAmenity hsa = HouseSharedAmenity.builder()
                .id(UUID.randomUUID().toString())
                .houseId(houseId)
                .amenityId(request.amenityId())
                .quantity(request.quantity() != null ? request.quantity() : 1)
                .notes(request.notes())
                .createdAt(LocalDateTime.now())
                .build();

        houseSharedAmenityRepository.save(hsa);
        hsa.setAmenity(amenity);

        return HouseSharedAmenityDto.fromEntity(hsa);
    }

    @Transactional
    public void removeHouseSharedAmenity(String houseId, String amenityId, String ownerId) {
        validateHouseAccess(houseId, ownerId);
        houseSharedAmenityRepository.deleteByHouseIdAndAmenityId(houseId, amenityId);
    }

    @Transactional
    public List<HouseSharedAmenityDto> setHouseSharedAmenities(String houseId, List<String> amenityIds,
            String ownerId) {
        validateHouseAccess(houseId, ownerId);

        // Delete existing
        houseSharedAmenityRepository.deleteByHouseId(houseId);

        // Add new ones
        LocalDateTime now = LocalDateTime.now();
        List<HouseSharedAmenity> newAmenities = amenityIds.stream()
                .map(amenityId -> HouseSharedAmenity.builder()
                        .id(UUID.randomUUID().toString())
                        .houseId(houseId)
                        .amenityId(amenityId)
                        .createdAt(now)
                        .build())
                .toList();

        houseSharedAmenityRepository.saveAll(newAmenities);

        // Return with enriched data
        return getHouseSharedAmenities(houseId, ownerId);
    }

    // ============ HELPERS ============

    private void validateHouseAccess(String houseId, String ownerId) {
        House house = houseRepository.findById(houseId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }
    }

    private void validateRoomAccess(String roomId, String ownerId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        House house = houseRepository.findById(room.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(ownerId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }
    }
}
