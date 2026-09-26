package com.example.ticketai.web.dto;

import com.example.ticketai.domain.Comment;
import java.time.Instant;

public record CommentResponse(Long id, String body, String author, Instant createdAt) {
    public static CommentResponse from(Comment comment) {
        return new CommentResponse(
                comment.getId(), comment.getBody(), comment.getAuthor(), comment.getCreatedAt());
    }
}
