package com.pegacorn.rently.dto.house;

import java.math.BigDecimal;

public record HouseStatsDto(
        String id,
        String name,
        int totalRooms,
        int rentedRooms,
        int vacantRooms,
        int debtRooms,
        int expiringContracts,
        int totalTenants,
        int unregisteredTenants,
        int missingInfoTenants,
        BigDecimal deposit,
        BigDecimal debt,
        BigDecimal paid,
        BigDecimal expense
) {}
