package com.example.ticketai.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentRequest(
        @NotBlank(message = "Comment body is required")
        @Size(max = 2000, message = "Comment must be at most 2000 characters")
        String body,

        @Size(max = 100, message = "Author must be at most 100 characters")
        String author) {
}
