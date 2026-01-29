package com.pegacorn.rently.dto.setting;

public record UpdateSettingRequest(
    String value,
    String description
) {}
