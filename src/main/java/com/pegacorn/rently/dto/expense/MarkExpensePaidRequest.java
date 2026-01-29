package com.pegacorn.rently.dto.expense;

import com.pegacorn.rently.entity.Expense;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MarkExpensePaidRequest(
        @NotNull(message = "Phương thức thanh toán không được để trống")
        Expense.PaymentMethod paymentMethod,

        @Size(max = 100, message = "Mã giao dịch không được quá 100 ký tự")
        String paymentReference
) {}
