package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.ServiceFee;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceFeeRepository extends JpaRepository<ServiceFee, String> {

    List<ServiceFee> findByHouseIdOrderByDisplayOrderAsc(String houseId);

    List<ServiceFee> findByHouseIdAndIsActiveTrueOrderByDisplayOrderAsc(String houseId);

    boolean existsByHouseIdAndNameAndIdNot(String houseId, String name, String id);

    boolean existsByHouseIdAndName(String houseId, String name);
}
