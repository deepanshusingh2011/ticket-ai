package com.example.ticketai.service;

import com.example.ticketai.domain.Comment;
import com.example.ticketai.domain.Priority;
import com.example.ticketai.domain.Ticket;
import com.example.ticketai.domain.TicketStatus;
import com.example.ticketai.repository.CommentRepository;
import com.example.ticketai.repository.TicketRepository;
import com.example.ticketai.web.dto.CommentRequest;
import com.example.ticketai.web.dto.TicketRequest;
import com.example.ticketai.web.dto.TicketUpdateRequest;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@Transactional
public class TicketService {

    private final TicketRepository tickets;
    private final CommentRepository comments;

    public TicketService(TicketRepository tickets, CommentRepository comments) {
        this.tickets = tickets;
        this.comments = comments;
    }

    public Ticket create(TicketRequest req) {
        Ticket ticket = new Ticket(
                req.title().trim(),
                req.description().trim(),
                req.priority() != null ? req.priority() : Priority.MEDIUM,
                normalizeToNull(req.assignee()));
        return tickets.save(ticket);
    }

    @Transactional(readOnly = true)
    public List<Ticket> list(TicketStatus status, String keyword) {
        boolean hasStatus = status != null;
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasStatus && hasKeyword) {
            String kw = keyword.trim().toLowerCase();
            return tickets.searchByKeyword(keyword.trim()).stream()
                    .filter(t -> t.getStatus() == status)
                    .toList();
        }
        if (hasKeyword) {
            return tickets.searchByKeyword(keyword.trim());
        }
        if (hasStatus) {
            return tickets.findByStatus(status);
        }
        return tickets.findAll();
    }

    @Transactional(readOnly = true)
    public Page<Ticket> listPaged(TicketStatus status, String keyword, Pageable pageable) {
        boolean hasStatus = status != null;
        boolean hasKeyword = StringUtils.hasText(keyword);
        if (hasStatus && hasKeyword) {
            return tickets.searchByKeywordAndStatus(keyword.trim(), status, pageable);
        }
        if (hasKeyword) {
            return tickets.searchByKeyword(keyword.trim(), pageable);
        }
        if (hasStatus) {
            return tickets.findByStatus(status, pageable);
        }
        return tickets.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Ticket get(Long id) {
        return tickets.findById(id)
                .orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
    }

    public Ticket update(Long id, TicketUpdateRequest req) {
        Ticket ticket = get(id);
        if (req.title() != null) {
            ticket.setTitle(req.title().trim());
        }
        if (req.description() != null) {
            ticket.setDescription(req.description().trim());
        }
        if (req.priority() != null) {
            ticket.setPriority(req.priority());
        }
        if (req.assignee() != null) {
            String a = req.assignee().trim();
            ticket.setAssignee(a.isEmpty() ? null : a);
        }
        ticket.touch();
        return tickets.save(ticket);
    }

    public Ticket changeStatus(Long id, TicketStatus target) {
        Ticket ticket = get(id);
        TicketStatus current = ticket.getStatus();
        if (current == target) {
            return ticket;
        }
        if (!current.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(
                    "Invalid status transition from " + current + " to " + target
                    + ". Allowed: OPEN -> IN_PROGRESS | CANCELLED; "
                    + "IN_PROGRESS -> RESOLVED | CANCELLED; RESOLVED -> CLOSED.");
        }
        ticket.setStatus(target);
        ticket.touch();
        return tickets.save(ticket);
    }

    public Comment addComment(Long ticketId, CommentRequest req) {
        Ticket ticket = get(ticketId);
        Comment comment = new Comment(ticket, req.body().trim(), normalizeToNull(req.author()));
        Comment saved = comments.save(comment);
        ticket.getComments().add(saved);
        ticket.touch();
        tickets.save(ticket);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<Comment> listComments(Long ticketId) {
        get(ticketId);
        return comments.findByTicketIdOrderByCreatedAtAsc(ticketId);
    }

    public void delete(Long id) {
        Ticket ticket = get(id);
        tickets.delete(ticket);
    }

    private static String normalizeToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
