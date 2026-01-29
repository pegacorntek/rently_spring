package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.ContractTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ContractTemplateRepository extends JpaRepository<ContractTemplate, String> {

    List<ContractTemplate> findByOwnerId(String ownerId);

    List<ContractTemplate> findByOwnerIdAndHouseIdIsNull(String ownerId);

    List<ContractTemplate> findByOwnerIdAndHouseId(String ownerId, String houseId);

    @Query("SELECT ct FROM ContractTemplate ct WHERE ct.ownerId = :ownerId AND (ct.houseId IS NULL OR ct.houseId = :houseId)")
    List<ContractTemplate> findByOwnerIdAndHouseIdOrGlobal(@Param("ownerId") String ownerId, @Param("houseId") String houseId);

    Optional<ContractTemplate> findByOwnerIdAndHouseIdAndIsDefaultTrue(String ownerId, String houseId);

    Optional<ContractTemplate> findByOwnerIdAndHouseIdIsNullAndIsDefaultTrue(String ownerId);

    @Modifying
    @Query("UPDATE ContractTemplate ct SET ct.isDefault = false WHERE ct.ownerId = :ownerId AND ct.houseId = :houseId AND ct.id != :excludeId")
    void clearDefaultForHouse(@Param("ownerId") String ownerId, @Param("houseId") String houseId, @Param("excludeId") String excludeId);

    @Modifying
    @Query("UPDATE ContractTemplate ct SET ct.isDefault = false WHERE ct.ownerId = :ownerId AND ct.houseId IS NULL AND ct.id != :excludeId")
    void clearDefaultForGlobal(@Param("ownerId") String ownerId, @Param("excludeId") String excludeId);
}
