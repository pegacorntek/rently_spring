package com.pegacorn.rently.dto.contracttemplate;

import jakarta.validation.constraints.Size;

public record UpdateContractTemplateRequest(
    @Size(max = 200, message = "Tên mẫu tối đa 200 ký tự")
    String name,

    String content,

    Boolean isDefault
) {}
