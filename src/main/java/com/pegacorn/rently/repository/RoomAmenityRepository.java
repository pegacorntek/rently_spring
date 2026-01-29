package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.RoomAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomAmenityRepository extends JpaRepository<RoomAmenity, String> {

    List<RoomAmenity> findByRoomId(String roomId);

    Optional<RoomAmenity> findByRoomIdAndAmenityId(String roomId, String amenityId);

    @Modifying
    void deleteByRoomIdAndAmenityId(String roomId, String amenityId);

    @Modifying
    void deleteByRoomId(String roomId);

    boolean existsByRoomIdAndAmenityId(String roomId, String amenityId);
}
