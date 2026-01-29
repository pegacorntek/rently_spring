package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Contract;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractRepository extends JpaRepository<Contract, String> {

    List<Contract> findByLandlordId(String landlordId);

    List<Contract> findByLandlordIdAndStatus(String landlordId, Contract.ContractStatus status);

    List<Contract> findByTenantId(String tenantId);

    List<Contract> findByRoomId(String roomId);

    Optional<Contract> findByRoomIdAndStatus(String roomId, Contract.ContractStatus status);

    // Find current contract (ACTIVE first, then DRAFT if no active)
    @Query("SELECT c FROM Contract c WHERE c.roomId = :roomId AND c.status IN ('ACTIVE', 'DRAFT') ORDER BY CASE c.status WHEN 'ACTIVE' THEN 0 ELSE 1 END")
    List<Contract> findCurrentContractsByRoomId(@Param("roomId") String roomId);

    @Query("SELECT c FROM Contract c JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId")
    List<Contract> findByHouseId(@Param("houseId") String houseId);

    @Query("SELECT c FROM Contract c JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND c.status = :status")
    List<Contract> findByHouseIdAndStatus(@Param("houseId") String houseId, @Param("status") Contract.ContractStatus status);

    boolean existsByRoomIdAndStatus(String roomId, Contract.ContractStatus status);

    List<Contract> findByLandlordIdAndDepositPaidFalse(String landlordId);

    @Query("SELECT c FROM Contract c JOIN Room r ON r.id = c.roomId WHERE r.houseId = :houseId AND c.depositPaid = false")
    List<Contract> findByHouseIdAndDepositPaidFalse(@Param("houseId") String houseId);
}
