package com.example.ticketai.web.dto;

import com.example.ticketai.domain.Priority;
import com.example.ticketai.domain.Ticket;
import com.example.ticketai.domain.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketStatus status,
        Priority priority,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        List<CommentResponse> comments) {

    public static TicketResponse from(Ticket ticket) {
        List<CommentResponse> commentDtos = ticket.getComments() == null ? List.of()
                : ticket.getComments().stream().map(CommentResponse::from).toList();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                commentDtos);
    }
}
