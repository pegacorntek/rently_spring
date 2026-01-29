package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.UtilityShortfall;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UtilityShortfallRepository extends JpaRepository<UtilityShortfall, String> {

    List<UtilityShortfall> findByHouseIdAndStatus(String houseId, UtilityShortfall.Status status);

    Optional<UtilityShortfall> findByHouseIdAndPeriodMonth(String houseId, String periodMonth);

    List<UtilityShortfall> findByHouseIdOrderByCreatedAtDesc(String houseId);
}
