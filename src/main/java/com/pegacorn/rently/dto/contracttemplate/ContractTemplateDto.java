package com.pegacorn.rently.dto.contracttemplate;

import java.time.LocalDateTime;

public record ContractTemplateDto(
    String id,
    String name,
    String houseId,
    String houseName,
    String content,
    Boolean isDefault,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {}
