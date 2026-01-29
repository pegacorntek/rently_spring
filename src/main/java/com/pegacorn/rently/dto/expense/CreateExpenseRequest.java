package com.pegacorn.rently.dto.expense;

import com.pegacorn.rently.entity.Expense;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record CreateExpenseRequest(
        @NotBlank(message = "House ID không được để trống")
        String houseId,

        @NotBlank(message = "Category ID không được để trống")
        String categoryId,

        @NotBlank(message = "Tiêu đề không được để trống")
        @Size(max = 255, message = "Tiêu đề không được quá 255 ký tự")
        String title,

        String description,

        @NotNull(message = "Số tiền không được để trống")
        @DecimalMin(value = "0", inclusive = false, message = "Số tiền phải lớn hơn 0")
        BigDecimal amount,

        @NotBlank(message = "Ngày chi không được để trống")
        String expenseDate,

        Expense.ExpenseStatus status,

        Expense.PaymentMethod paymentMethod
) {}
