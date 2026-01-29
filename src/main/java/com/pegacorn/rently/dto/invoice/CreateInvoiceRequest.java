package com.pegacorn.rently.dto.invoice;

import com.pegacorn.rently.entity.InvoiceItem;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;

public record CreateInvoiceRequest(
                @NotBlank(message = "Contract ID is required") String contractId,

                @NotBlank(message = "Period month is required") String periodMonth,

                @NotBlank(message = "Due date is required") String dueDate,

                @NotEmpty(message = "At least one item is required") @Valid List<ItemRequest> items,

                @DecimalMin(value = "0", message = "Late fee percent cannot be negative") BigDecimal lateFeePercent,

                // Optional: defaults to NORMAL if not provided
                String invoiceType) {
        public record ItemRequest(
                        @NotNull(message = "Item type is required") InvoiceItem.InvoiceItemType type,

                        @NotBlank(message = "Description is required") String description,

                        @NotNull(message = "Quantity is required") @DecimalMin(value = "0.01", message = "Quantity must be greater than 0") BigDecimal quantity,

                        @NotNull(message = "Unit price is required") @DecimalMin(value = "0", message = "Unit price cannot be negative") BigDecimal unitPrice,

                        @NotNull(message = "Amount is required") @DecimalMin(value = "0", message = "Amount cannot be negative") BigDecimal amount) {
        }
}
