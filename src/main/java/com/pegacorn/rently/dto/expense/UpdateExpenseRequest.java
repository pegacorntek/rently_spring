package com.pegacorn.rently.dto.expense;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record UpdateExpenseRequest(
        String categoryId,

        @Size(max = 255, message = "Tiêu đề không được quá 255 ký tự")
        String title,

        String description,

        @DecimalMin(value = "0", inclusive = false, message = "Số tiền phải lớn hơn 0")
        BigDecimal amount,

        String expenseDate
) {}
