package com.example.ticketai.web;

import com.example.ticketai.domain.Ticket;
import com.example.ticketai.domain.TicketStatus;
import com.example.ticketai.domain.Comment;
import com.example.ticketai.service.TicketService;
import com.example.ticketai.web.dto.CommentRequest;
import com.example.ticketai.web.dto.CommentResponse;
import com.example.ticketai.web.dto.StatusChangeRequest;
import com.example.ticketai.web.dto.TicketRequest;
import com.example.ticketai.web.dto.TicketResponse;
import com.example.ticketai.web.dto.TicketUpdateRequest;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/tickets")
@CrossOrigin(origins = {"http://localhost:5173", "http://localhost:3000"})
public class TicketController {

    private final TicketService service;

    public TicketController(TicketService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketRequest request) {
        Ticket created = service.create(request);
        return ResponseEntity.created(URI.create("/api/tickets/" + created.getId()))
                .body(TicketResponse.from(created));
    }

    @GetMapping
    public List<TicketResponse> list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false, name = "q") String keyword) {
        return service.list(status, keyword).stream().map(TicketResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TicketResponse get(@PathVariable Long id) {
        return TicketResponse.from(service.get(id));
    }

    @PutMapping("/{id}")
    public TicketResponse update(
            @PathVariable Long id, @Valid @RequestBody TicketUpdateRequest request) {
        return TicketResponse.from(service.update(id, request));
    }

    @PatchMapping("/{id}/status")
    public TicketResponse changeStatus(
            @PathVariable Long id, @Valid @RequestBody StatusChangeRequest request) {
        return TicketResponse.from(service.changeStatus(id, request.status()));
    }

    @PostMapping("/{id}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long id, @Valid @RequestBody CommentRequest request) {
        Comment saved = service.addComment(id, request);
        return ResponseEntity.created(URI.create("/api/tickets/" + id + "/comments/" + saved.getId()))
                .body(CommentResponse.from(saved));
    }

    @GetMapping("/{id}/comments")
    public List<CommentResponse> listComments(@PathVariable Long id) {
        return service.listComments(id).stream().map(CommentResponse::from).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
