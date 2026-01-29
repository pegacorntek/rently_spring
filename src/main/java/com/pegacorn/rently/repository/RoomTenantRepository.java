package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.RoomTenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomTenantRepository extends JpaRepository<RoomTenant, String> {

        @Query("SELECT new com.pegacorn.rently.entity.RoomTenant(rt.id, rt.roomId, rt.userId, rt.isPrimary, rt.joinedAt, rt.leftAt, "
                        +
                        "u.fullName, u.phone, u.idNumber, u.gender, u.dateOfBirth, u.placeOfOrigin, u.idIssueDate, u.idIssuePlace, "
                        +
                        "u.shareFullName, u.sharePhone, u.shareOrigin, u.shareGender) "
                        +
                        "FROM RoomTenant rt JOIN User u ON u.id = rt.userId WHERE rt.roomId = :roomId AND rt.leftAt IS NULL")
        List<RoomTenant> findActiveByRoomId(@Param("roomId") String roomId);

        Optional<RoomTenant> findByRoomIdAndUserIdAndLeftAtIsNull(String roomId, String userId);

        @Modifying
        @Query("UPDATE RoomTenant rt SET rt.leftAt = :leftAt WHERE rt.roomId = :roomId AND rt.userId = :userId AND rt.leftAt IS NULL")
        void markAsLeft(@Param("roomId") String roomId, @Param("userId") String userId,
                        @Param("leftAt") LocalDateTime leftAt);

        @Modifying
        @Query("UPDATE RoomTenant rt SET rt.leftAt = :leftAt WHERE rt.roomId = :roomId AND rt.leftAt IS NULL")
        void markAllAsLeft(@Param("roomId") String roomId, @Param("leftAt") LocalDateTime leftAt);

        boolean existsByRoomIdAndUserIdAndLeftAtIsNull(String roomId, String userId);

        @Modifying
        @Query("UPDATE RoomTenant rt SET rt.isPrimary = false WHERE rt.roomId = :roomId AND rt.leftAt IS NULL")
        void clearPrimaryForRoom(@Param("roomId") String roomId);

        @Modifying
        @Query("UPDATE RoomTenant rt SET rt.isPrimary = true WHERE rt.roomId = :roomId AND rt.userId = :userId AND rt.leftAt IS NULL")
        void setPrimary(@Param("roomId") String roomId, @Param("userId") String userId);

        @Query("SELECT new com.pegacorn.rently.entity.RoomTenant(rt.id, rt.roomId, rt.userId, rt.isPrimary, rt.joinedAt, rt.leftAt, "
                        +
                        "u.fullName, u.phone, u.idNumber, u.gender, u.dateOfBirth, u.placeOfOrigin, u.idIssueDate, u.idIssuePlace, "
                        +
                        "u.shareFullName, u.sharePhone, u.shareOrigin, u.shareGender) "
                        +
                        "FROM RoomTenant rt JOIN User u ON u.id = rt.userId WHERE rt.roomId IN :roomIds AND rt.leftAt IS NULL")
        List<RoomTenant> findActiveWithUserByRoomIds(@Param("roomIds") List<String> roomIds);

        @Query("SELECT rt FROM RoomTenant rt WHERE rt.roomId IN :roomIds AND rt.leftAt IS NULL")
        List<RoomTenant> findActiveByRoomIds(@Param("roomIds") List<String> roomIds);

        List<RoomTenant> findByRoomIdAndLeftAtIsNull(String roomId);

        @Query("SELECT rt FROM RoomTenant rt WHERE rt.roomId = :roomId AND rt.isPrimary = true AND rt.leftAt IS NULL")
        Optional<RoomTenant> findPrimaryByRoomId(@Param("roomId") String roomId);

        @Query("SELECT COUNT(rt) > 0 FROM RoomTenant rt, Room r, House h, User u " +
                        "WHERE rt.userId = u.id AND rt.roomId = r.id AND r.houseId = h.id " +
                        "AND u.phone = :phone AND h.ownerId = :ownerId AND rt.leftAt IS NULL")
        boolean isTenantOfLandlord(@Param("phone") String phone, @Param("ownerId") String ownerId);
}
