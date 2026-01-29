package com.pegacorn.rently.service;

import com.pegacorn.rently.constant.MessageConstant;
import com.pegacorn.rently.dto.ticket.TicketDto;
import com.pegacorn.rently.dto.ticket.UpdateTicketRequest;
import com.pegacorn.rently.entity.*;
import com.pegacorn.rently.entity.Notification;
import com.pegacorn.rently.exception.ApiException;
import com.pegacorn.rently.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final RoomRepository roomRepository;
    private final HouseRepository houseRepository;
    private final UserRepository userRepository;
    private final RoomTenantRepository roomTenantRepository;
    private final NotificationService notificationService;

    @Value("${upload.path:./uploads}")
    private String uploadPath;

    public List<TicketDto> getAllByLandlord(String landlordId, String houseId, String status) {
        List<Ticket> tickets;

        if (houseId != null && status != null) {
            tickets = ticketRepository.findByHouseIdAndStatus(houseId, Ticket.TicketStatus.valueOf(status));
        } else if (houseId != null) {
            tickets = ticketRepository.findByHouseId(houseId);
        } else if (status != null) {
            tickets = ticketRepository.findByLandlordIdAndStatus(landlordId, Ticket.TicketStatus.valueOf(status));
        } else {
            tickets = ticketRepository.findByLandlordId(landlordId);
        }

        return tickets.stream()
                .map(this::enrichTicket)
                .map(TicketDto::fromEntity)
                .toList();
    }

    public TicketDto getById(String id, String landlordId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TICKET_NOT_FOUND));

        House house = houseRepository.findById(ticket.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        return TicketDto.fromEntity(enrichTicket(ticket));
    }

    @Transactional
    public TicketDto updateStatus(String id, UpdateTicketRequest request, String landlordId) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.TICKET_NOT_FOUND));

        House house = houseRepository.findById(ticket.getHouseId())
                .orElseThrow(() -> ApiException.notFound(MessageConstant.HOUSE_NOT_FOUND));

        if (!house.getOwnerId().equals(landlordId)) {
            throw ApiException.forbidden(MessageConstant.ACCESS_DENIED);
        }

        Ticket.TicketStatus oldStatus = ticket.getStatus();
        ticket.setStatus(request.status());
        ticket.setUpdatedAt(LocalDateTime.now());
        ticketRepository.save(ticket);

        // Notify tenant if ticket is resolved
        if (request.status() == Ticket.TicketStatus.DONE && oldStatus != Ticket.TicketStatus.DONE) {
            notifyTenantTicketResolved(ticket);
        }

        return TicketDto.fromEntity(enrichTicket(ticket));
    }

    public List<TicketDto> getMyTickets(String tenantId) {
        List<Ticket> tickets = ticketRepository.findByTenantId(tenantId);
        return tickets.stream()
                .map(this::enrichTicket)
                .map(TicketDto::fromEntity)
                .toList();
    }

    @Transactional
    public TicketDto create(String roomId, String title, String description,
                           List<MultipartFile> attachments, String tenantId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> ApiException.notFound(MessageConstant.ROOM_NOT_FOUND));

        // Verify tenant is in this room
        if (!roomTenantRepository.existsByRoomIdAndUserIdAndLeftAtIsNull(roomId, tenantId)) {
            throw ApiException.forbidden(MessageConstant.NOT_TENANT_OF_ROOM);
        }

        Ticket ticket = Ticket.builder()
                .id(UUID.randomUUID().toString())
                .houseId(room.getHouseId())
                .roomId(roomId)
                .tenantId(tenantId)
                .title(title)
                .description(description)
                .status(Ticket.TicketStatus.OPEN)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        ticketRepository.save(ticket);

        if (attachments != null && !attachments.isEmpty()) {
            for (MultipartFile file : attachments) {
                String fileName = saveFile(file, "tickets");
                String fileUrl = "/files/tickets/" + fileName;

                TicketAttachment.AttachmentType type = determineAttachmentType(file);

                TicketAttachment attachment = TicketAttachment.builder()
                        .id(UUID.randomUUID().toString())
                        .ticketId(ticket.getId())
                        .type(type)
                        .fileUrl(fileUrl)
                        .createdAt(LocalDateTime.now())
                        .build();

                ticketAttachmentRepository.save(attachment);
            }
        }

        // Notify landlord about new ticket
        notifyLandlordNewTicket(ticket, room);

        return TicketDto.fromEntity(enrichTicket(ticket));
    }

    private void notifyLandlordNewTicket(Ticket ticket, Room room) {
        try {
            House house = houseRepository.findById(room.getHouseId()).orElse(null);
            if (house == null) return;

            User tenant = userRepository.findById(ticket.getTenantId()).orElse(null);
            String tenantName = tenant != null ? tenant.getFullName() : "Khách thuê";

            String notifTitle = "Báo cáo sự cố mới - Phòng " + room.getCode();
            String notifBody = String.format("%s đã báo cáo: \"%s\"", tenantName, ticket.getTitle());

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("ticketId", ticket.getId());
            data.put("url", "/landlord/tickets/" + ticket.getId());

            notificationService.createNotification(
                    house.getOwnerId(),
                    Notification.NotificationType.MAINTENANCE_REQUEST,
                    notifTitle,
                    notifBody,
                    data);
        } catch (Exception e) {
            // Log but don't fail the main operation
        }
    }

    private void notifyTenantTicketResolved(Ticket ticket) {
        try {
            Room room = roomRepository.findById(ticket.getRoomId()).orElse(null);
            String roomCode = room != null ? room.getCode() : "";

            String notifTitle = "Sự cố đã được xử lý";
            String notifBody = String.format("Yêu cầu \"%s\" tại phòng %s đã được xử lý.", ticket.getTitle(), roomCode);

            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("ticketId", ticket.getId());
            data.put("url", "/tenant/tickets/" + ticket.getId());

            notificationService.createNotification(
                    ticket.getTenantId(),
                    Notification.NotificationType.MAINTENANCE_REQUEST,
                    notifTitle,
                    notifBody,
                    data);
        } catch (Exception e) {
            // Log but don't fail the main operation
        }
    }

    private Ticket enrichTicket(Ticket ticket) {
        ticket.setAttachments(ticketAttachmentRepository.findByTicketId(ticket.getId()));

        Room room = roomRepository.findById(ticket.getRoomId()).orElse(null);
        if (room != null) {
            House house = houseRepository.findById(room.getHouseId()).orElse(null);
            ticket.setRoom(Ticket.RoomInfo.builder()
                    .code(room.getCode())
                    .houseName(house != null ? house.getName() : null)
                    .build());
        }

        User tenant = userRepository.findById(ticket.getTenantId()).orElse(null);
        if (tenant != null) {
            ticket.setTenant(Ticket.TenantInfo.builder()
                    .fullName(tenant.getFullName())
                    .phone(tenant.getPhone())
                    .build());
        }

        return ticket;
    }

    private String saveFile(MultipartFile file, String folder) {
        try {
            Path uploadDir = Paths.get(uploadPath, folder);
            Files.createDirectories(uploadDir);

            String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
            Path filePath = uploadDir.resolve(fileName);
            Files.copy(file.getInputStream(), filePath);

            return fileName;
        } catch (IOException e) {
            throw ApiException.badRequest(MessageConstant.FAILED_TO_SAVE_FILE + e.getMessage());
        }
    }

    private TicketAttachment.AttachmentType determineAttachmentType(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video/")) {
            return TicketAttachment.AttachmentType.VIDEO;
        }
        return TicketAttachment.AttachmentType.IMAGE;
    }
}
