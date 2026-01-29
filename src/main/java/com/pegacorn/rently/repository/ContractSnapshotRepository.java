package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.ContractSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ContractSnapshotRepository extends JpaRepository<ContractSnapshot, String> {

    List<ContractSnapshot> findByContractIdOrderByCreatedAtDesc(String contractId);

    // For tenant: only get snapshots from their start date
    @Query("SELECT s FROM ContractSnapshot s WHERE s.contractId = :contractId AND s.createdAt >= :fromDate ORDER BY s.createdAt DESC")
    List<ContractSnapshot> findByContractIdFromDate(@Param("contractId") String contractId, @Param("fromDate") LocalDateTime fromDate);

    // Count snapshots for a contract
    long countByContractId(String contractId);

    // Get oldest snapshots (for cleanup when exceeding limit)
    @Query("SELECT s FROM ContractSnapshot s WHERE s.contractId = :contractId ORDER BY s.createdAt ASC")
    List<ContractSnapshot> findByContractIdOrderByCreatedAtAsc(String contractId);
}
