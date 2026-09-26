package com.example.ticketai.web.dto;

import com.example.ticketai.domain.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record StatusChangeRequest(
        @NotNull(message = "Status is required")
        TicketStatus status) {
}
