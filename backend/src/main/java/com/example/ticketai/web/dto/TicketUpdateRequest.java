package com.example.ticketai.web.dto;

import com.example.ticketai.domain.Priority;
import jakarta.validation.constraints.Size;

public record TicketUpdateRequest(
        @Size(min = 1, max = 200, message = "Title must be between 1 and 200 characters")
        String title,

        @Size(min = 1, max = 4000, message = "Description must be between 1 and 4000 characters")
        String description,

        Priority priority,

        @Size(max = 100, message = "Assignee must be at most 100 characters")
        String assignee) {
}
