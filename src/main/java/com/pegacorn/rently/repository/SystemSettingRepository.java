package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.SystemSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemSettingRepository extends JpaRepository<SystemSetting, String> {
    Optional<SystemSetting> findByKey(String key);
    boolean existsByKey(String key);
}
