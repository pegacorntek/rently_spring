package com.pegacorn.rently.dto.expense;

import com.pegacorn.rently.entity.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ExpenseDto(
        String id,
        String houseId,
        String houseName,
        String categoryId,
        String categoryName,
        String categoryIcon,
        String title,
        String description,
        BigDecimal amount,
        LocalDate expenseDate,
        String receiptUrl,
        String status,
        LocalDateTime paidAt,
        String paymentMethod,
        String paymentReference,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static ExpenseDto from(Expense expense) {
        return new ExpenseDto(
                expense.getId(),
                expense.getHouseId(),
                expense.getHouseName(),
                expense.getCategoryId(),
                expense.getCategoryName(),
                expense.getCategoryIcon(),
                expense.getTitle(),
                expense.getDescription(),
                expense.getAmount(),
                expense.getExpenseDate(),
                expense.getReceiptUrl(),
                expense.getStatus() != null ? expense.getStatus().name() : null,
                expense.getPaidAt(),
                expense.getPaymentMethod() != null ? expense.getPaymentMethod().name() : null,
                expense.getPaymentReference(),
                expense.getCreatedAt(),
                expense.getUpdatedAt()
        );
    }
}
