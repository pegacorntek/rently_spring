package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.HouseSharedAmenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HouseSharedAmenityRepository extends JpaRepository<HouseSharedAmenity, String> {

    List<HouseSharedAmenity> findByHouseId(String houseId);

    Optional<HouseSharedAmenity> findByHouseIdAndAmenityId(String houseId, String amenityId);

    void deleteByHouseIdAndAmenityId(String houseId, String amenityId);

    void deleteByHouseId(String houseId);
}
