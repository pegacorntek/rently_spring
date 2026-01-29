package com.pegacorn.rently.dto.ticket;

import com.pegacorn.rently.entity.Ticket;
import com.pegacorn.rently.entity.TicketAttachment;

import java.time.LocalDateTime;
import java.util.List;

public record TicketDto(
        String id,
        String houseId,
        String roomId,
        RoomInfo room,
        String tenantId,
        TenantInfo tenant,
        String title,
        String description,
        List<AttachmentDto> attachments,
        String status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public record RoomInfo(String code, String houseName) {}
    public record TenantInfo(String fullName, String phone) {}

    public record AttachmentDto(String id, String type, String fileUrl) {
        public static AttachmentDto fromEntity(TicketAttachment attachment) {
            return new AttachmentDto(
                    attachment.getId(),
                    attachment.getType().name(),
                    attachment.getFileUrl()
            );
        }
    }

    public static TicketDto fromEntity(Ticket ticket) {
        RoomInfo roomInfo = ticket.getRoom() != null
                ? new RoomInfo(ticket.getRoom().getCode(), ticket.getRoom().getHouseName())
                : null;

        TenantInfo tenantInfo = ticket.getTenant() != null
                ? new TenantInfo(ticket.getTenant().getFullName(), ticket.getTenant().getPhone())
                : null;

        List<AttachmentDto> attachmentDtos = ticket.getAttachments() != null
                ? ticket.getAttachments().stream().map(AttachmentDto::fromEntity).toList()
                : null;

        return new TicketDto(
                ticket.getId(),
                ticket.getHouseId(),
                ticket.getRoomId(),
                roomInfo,
                ticket.getTenantId(),
                tenantInfo,
                ticket.getTitle(),
                ticket.getDescription(),
                attachmentDtos,
                ticket.getStatus().name(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }
}
