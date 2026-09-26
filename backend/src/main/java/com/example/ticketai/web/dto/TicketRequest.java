package com.example.ticketai.web.dto;

import com.example.ticketai.domain.Priority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TicketRequest(
        @NotBlank(message = "Title is required")
        @Size(max = 200, message = "Title must be at most 200 characters")
        String title,

        @NotBlank(message = "Description is required")
        @Size(max = 4000, message = "Description must be at most 4000 characters")
        String description,

        Priority priority,

        @Size(max = 100, message = "Assignee must be at most 100 characters")
        String assignee) {
}
