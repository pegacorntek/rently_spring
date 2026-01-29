package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "meter_readings")
public class MeterReading {
    @Id
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "period_month", nullable = false, length = 7)
    private String periodMonth;

    @Column(name = "electricity_old", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityOld;

    @Column(name = "electricity_new", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityNew;

    @Column(name = "electricity_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal electricityUnitPrice;

    @Column(name = "water_old", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterOld;

    @Column(name = "water_new", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterNew;

    @Column(name = "water_unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal waterUnitPrice;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
