package com.pegacorn.rently.dto.invoice;

import com.pegacorn.rently.entity.Invoice;
import com.pegacorn.rently.entity.InvoiceItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record InvoiceDto(
                String id,
                String contractId,
                String tenantId,
                TenantInfo tenant,
                RoomInfo room,
                String periodMonth,
                String dueDate,
                List<ItemDto> items,
                BigDecimal totalAmount,
                BigDecimal paidAmount,
                BigDecimal lateFeePercent,
                String status,
                String invoiceType,
                Boolean isNetting,
                Boolean hasPendingPayment,
                LocalDateTime createdAt,
                LocalDateTime updatedAt) {
        public record TenantInfo(String fullName, String phone) {
        }

        public record RoomInfo(String code, String houseName) {
        }

        public record ItemDto(
                        String id,
                        String type,
                        String description,
                        BigDecimal quantity,
                        BigDecimal unitPrice,
                        BigDecimal amount) {
                public static ItemDto fromEntity(InvoiceItem item) {
                        return new ItemDto(
                                        item.getId(),
                                        item.getType().name(),
                                        item.getDescription(),
                                        item.getQuantity(),
                                        item.getUnitPrice(),
                                        item.getAmount());
                }
        }

        public static InvoiceDto fromEntity(Invoice invoice) {
                return fromEntity(invoice, false);
        }

        public static InvoiceDto fromEntity(Invoice invoice, boolean hasPendingPayment) {
                TenantInfo tenantInfo = invoice.getTenant() != null
                                ? new TenantInfo(invoice.getTenant().getFullName(), invoice.getTenant().getPhone())
                                : null;

                RoomInfo roomInfo = invoice.getRoom() != null
                                ? new RoomInfo(invoice.getRoom().getCode(), invoice.getRoom().getHouseName())
                                : null;

                List<ItemDto> itemDtos = invoice.getItems() != null
                                ? invoice.getItems().stream().map(ItemDto::fromEntity).toList()
                                : null;

                return new InvoiceDto(
                                invoice.getId(),
                                invoice.getContractId(),
                                invoice.getTenantId(),
                                tenantInfo,
                                roomInfo,
                                invoice.getPeriodMonth(),
                                invoice.getDueDate().toString(),
                                itemDtos,
                                invoice.getTotalAmount(),
                                invoice.getPaidAmount(),
                                invoice.getLateFeePercent(),
                                invoice.getStatus().name(),
                                invoice.getInvoiceType() != null ? invoice.getInvoiceType().name() : "NORMAL",
                                invoice.getIsNetting(),
                                hasPendingPayment,
                                invoice.getCreatedAt(),
                                invoice.getUpdatedAt());
        }
}
