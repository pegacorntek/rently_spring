package com.pegacorn.rently.repository;

import com.pegacorn.rently.entity.MeterReading;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MeterReadingRepository extends JpaRepository<MeterReading, String> {

    List<MeterReading> findByRoomId(String roomId);

    Optional<MeterReading> findByRoomIdAndPeriodMonth(String roomId, String periodMonth);

    List<MeterReading> findByRoomIdOrderByPeriodMonthDesc(String roomId);

    List<MeterReading> findByRoomIdInAndPeriodMonth(List<String> roomIds, String periodMonth);
}
