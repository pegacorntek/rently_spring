package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.Amenity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AmenityRepository extends JpaRepository<Amenity, String> {

    // Get all amenities for a specific house
    @Query("SELECT a FROM Amenity a WHERE a.houseId = :houseId ORDER BY a.category, a.name")
    List<Amenity> findByHouseId(String houseId);

    // Check if amenity belongs to house (for authorization)
    boolean existsByIdAndHouseId(String id, String houseId);
}
