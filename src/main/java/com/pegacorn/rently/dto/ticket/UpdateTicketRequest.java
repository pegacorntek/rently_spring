package com.pegacorn.rently.dto.ticket;

import com.pegacorn.rently.entity.Ticket;
import jakarta.validation.constraints.NotNull;

public record UpdateTicketRequest(
        @NotNull(message = "Status is required")
        Ticket.TicketStatus status
) {}
