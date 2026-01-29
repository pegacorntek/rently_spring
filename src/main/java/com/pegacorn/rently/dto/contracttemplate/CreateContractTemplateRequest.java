package com.pegacorn.rently.dto.contracttemplate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateContractTemplateRequest(
    @NotBlank(message = "Tên mẫu không được để trống")
    @Size(max = 200, message = "Tên mẫu tối đa 200 ký tự")
    String name,

    String houseId,

    @NotBlank(message = "Nội dung không được để trống")
    String content,

    Boolean isDefault
) {}
