package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.House;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface HouseRepository extends JpaRepository<House, String> {

    List<House> findByOwnerId(String ownerId);

    List<House> findByOwnerIdAndStatus(String ownerId, House.HouseStatus status);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.houseId = :houseId")
    int countRoomsByHouseId(@Param("houseId") String houseId);

    @Query("SELECT COUNT(DISTINCT rt.userId) FROM RoomTenant rt JOIN Room r ON r.id = rt.roomId WHERE r.houseId = :houseId AND rt.leftAt IS NULL")
    int countTenantsByHouseId(@Param("houseId") String houseId);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.houseId = :houseId AND r.status = 'RENTED'")
    int countRentedRoomsByHouseId(@Param("houseId") String houseId);

    @Query("SELECT COUNT(r) FROM Room r WHERE r.houseId = :houseId AND r.status = 'EMPTY'")
    int countVacantRoomsByHouseId(@Param("houseId") String houseId);

    // Rooms with unpaid invoices
    @Query("SELECT COUNT(DISTINCT c.roomId) FROM Invoice i JOIN Contract c ON c.id = i.contractId JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND i.status IN ('DRAFT', 'PARTIALLY_PAID', 'OVERDUE')")
    int countDebtRoomsByHouseId(@Param("houseId") String houseId);

    // Contracts expiring within 30 days
    @Query("SELECT COUNT(c) FROM Contract c JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND c.status = 'ACTIVE' AND c.endDate <= :expiryDate")
    int countExpiringContractsByHouseId(@Param("houseId") String houseId, @Param("expiryDate") LocalDate expiryDate);

    // Tenants missing ID number
    @Query("SELECT COUNT(DISTINCT rt.userId) FROM RoomTenant rt JOIN Room r ON r.id = rt.roomId JOIN User u ON u.id = rt.userId WHERE r.houseId = :houseId AND rt.leftAt IS NULL AND (u.idNumber IS NULL OR u.idNumber = '')")
    int countMissingInfoTenantsByHouseId(@Param("houseId") String houseId);

    // Total deposit from active contracts
    @Query("SELECT COALESCE(SUM(c.depositAmount), 0) FROM Contract c JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND c.status = 'ACTIVE'")
    BigDecimal sumDepositByHouseId(@Param("houseId") String houseId);

    // Total debt (unpaid invoice amount)
    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM Invoice i JOIN Contract c ON c.id = i.contractId JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND i.status IN ('DRAFT', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal sumDebtByHouseId(@Param("houseId") String houseId);

    // Total paid amount from all invoices
    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i JOIN Contract c ON c.id = i.contractId JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId")
    BigDecimal sumPaidByHouseId(@Param("houseId") String houseId);

    // Total paid amount filtered by period month (YYYY-MM)
    @Query("SELECT COALESCE(SUM(i.paidAmount), 0) FROM Invoice i JOIN Contract c ON c.id = i.contractId JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND i.periodMonth = :periodMonth")
    BigDecimal sumPaidByHouseIdAndPeriod(@Param("houseId") String houseId, @Param("periodMonth") String periodMonth);

    // Total debt filtered by period month (YYYY-MM)
    @Query("SELECT COALESCE(SUM(i.totalAmount - i.paidAmount), 0) FROM Invoice i JOIN Contract c ON c.id = i.contractId JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND i.periodMonth = :periodMonth AND i.status IN ('DRAFT', 'PARTIALLY_PAID', 'OVERDUE')")
    BigDecimal sumDebtByHouseIdAndPeriod(@Param("houseId") String houseId, @Param("periodMonth") String periodMonth);
}
