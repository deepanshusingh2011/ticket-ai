package com.example.ticketai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

@Entity
@Table(name = "comments")
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 2000)
    private String body;

    @Column(length = 100)
    private String author;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected Comment() {
    }

    public Comment(Ticket ticket, String body, String author) {
        this.ticket = ticket;
        this.body = body;
        this.author = author;
        this.createdAt = Instant.now();
    }

    public Long getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public String getBody() { return body; }
    public String getAuthor() { return author; }
    public Instant getCreatedAt() { return createdAt; }
}
