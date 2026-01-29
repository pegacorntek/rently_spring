package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Room;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, String> {

    List<Room> findByHouseId(String houseId);

    Page<Room> findByHouseIdOrderByCodeAsc(String houseId, Pageable pageable);

    List<Room> findByHouseIdAndStatus(String houseId, Room.RoomStatus status);

    Optional<Room> findByHouseIdAndCode(String houseId, String code);

    boolean existsByHouseIdAndCode(String houseId, String code);

    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM Invoice i JOIN Contract c ON c.id = i.contractId WHERE c.roomId = :roomId AND i.status NOT IN ('PAID', 'CANCELLED')")
    BigDecimal calculateDebtByRoomId(@Param("roomId") String roomId);

    @Query("SELECT r FROM Room r JOIN RoomTenant rt ON rt.roomId = r.id WHERE rt.userId = :userId AND rt.leftAt IS NULL")
    List<Room> findByTenantUserId(@Param("userId") String userId);

    List<Room> findByHouseIdIn(List<String> houseIds);

    @Query("SELECT r FROM Room r JOIN House h ON h.id = r.houseId WHERE h.ownerId = :ownerId")
    List<Room> findAllByLandlordId(@Param("ownerId") String ownerId);
}
