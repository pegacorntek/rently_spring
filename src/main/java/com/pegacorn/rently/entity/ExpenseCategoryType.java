package com.pegacorn.rently.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Standard fee/expense categories.
 * Used for:
 * - Expense categories (landlord expenses)
 * - Service fee presets (when adding to house/contract)
 * - Reconciliation matching (electric/water)
 *
 * Translation keys: expense.category.{id} or serviceFee.preset.{id}
 */
@Getter
@RequiredArgsConstructor
public enum ExpenseCategoryType {
    ELECTRIC("electric", "Tiền điện", "Electricity", "bolt", ReconciliationType.ELECTRICITY),
    WATER("water", "Tiền nước", "Water", "water_drop", ReconciliationType.WATER),
    INTERNET("internet", "Internet", "Internet", "wifi", null),
    TRASH("trash", "Rác", "Trash", "delete", null),
    SECURITY("security", "Bảo vệ", "Security", "security", null),
    PARKING("parking", "Gửi xe", "Parking", "local_parking", null),
    ELEVATOR("elevator", "Thang máy", "Elevator", "elevator", null),
    LAUNDRY("laundry", "Máy giặt công cộng", "Public Laundry", "local_laundry_service", null),
    OTHER("other", "Khác", "Other", "more_horiz", null);

    private final String id;
    private final String nameVi;
    private final String nameEn;
    private final String icon;
    private final ReconciliationType reconciliationType;

    public enum ReconciliationType {
        ELECTRICITY,
        WATER
    }

    public String getName(String lang) {
        return "en".equalsIgnoreCase(lang) ? nameEn : nameVi;
    }

    public static ExpenseCategoryType fromId(String id) {
        if (id == null) return null;
        for (ExpenseCategoryType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }

    public static boolean isValid(String id) {
        return fromId(id) != null;
    }

    /**
     * Check if this category contributes to electricity reconciliation
     */
    public boolean isElectricity() {
        return reconciliationType == ReconciliationType.ELECTRICITY;
    }

    /**
     * Check if this category contributes to water reconciliation
     */
    public boolean isWater() {
        return reconciliationType == ReconciliationType.WATER;
    }

    /**
     * Get category for electricity expenses
     */
    public static ExpenseCategoryType getElectricityCategory() {
        return ELECTRIC;
    }

    /**
     * Get category for water expenses
     */
    public static ExpenseCategoryType getWaterCategory() {
        return WATER;
    }
}
