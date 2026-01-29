package com.pegacorn.rently.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "ticket_attachments")
public class TicketAttachment {
    @Id
    private String id;

    @Column(name = "ticket_id", nullable = false)
    private String ticketId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttachmentType type;

    @Column(name = "file_url", nullable = false, length = 500)
    private String fileUrl;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum AttachmentType {
        IMAGE, VIDEO
    }
}
