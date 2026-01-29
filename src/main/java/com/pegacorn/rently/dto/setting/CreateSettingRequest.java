package com.pegacorn.rently.dto.setting;

public record CreateSettingRequest(
    String key,
    String value,
    String description,
    String valueType
) {}
