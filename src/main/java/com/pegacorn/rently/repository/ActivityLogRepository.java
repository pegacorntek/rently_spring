package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.ActivityLog;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ActivityLogRepository extends JpaRepository<ActivityLog, String>, JpaSpecificationExecutor<ActivityLog> {

    List<ActivityLog> findByLandlordIdOrderByCreatedAtDesc(String landlordId, Pageable pageable);

    List<ActivityLog> findByLandlordIdAndTypeOrderByCreatedAtDesc(String landlordId, ActivityLog.ActivityType type, Pageable pageable);
}
