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
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
@CrossOrigin(origins = {"http://localhost:5174", "http://localhost:5173", "http://localhost:3000"})
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
    public Object list(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size,
            @RequestParam(required = false) List<String> sort) {
        // Backward compat: no page/size params -> return full list (existing behavior).
        if (page == null && size == null && sort == null) {
            return service.list(status, keyword).stream().map(TicketResponse::from).toList();
        }
        int pageNumber = page != null ? page : 0;
        int pageSize = size != null ? size : 10;
        Pageable pageable = PageRequest.of(pageNumber, pageSize, parseSort(sort));
        Page<TicketResponse> result = service.listPaged(status, keyword, pageable).map(TicketResponse::from);
        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("content", result.getContent());
        envelope.put("page", result.getNumber());
        envelope.put("size", result.getSize());
        envelope.put("totalElements", result.getTotalElements());
        envelope.put("totalPages", result.getTotalPages());
        return envelope;
    }

    private static Sort parseSort(List<String> sort) {
        if (sort == null || sort.isEmpty()) {
            return Sort.by(Sort.Direction.DESC, "id");
        }
        List<Sort.Order> orders = new ArrayList<>();
        for (String entry : sort) {
            if (entry == null || entry.isBlank()) {
                continue;
            }
            String[] parts = entry.split(",");
            String property = parts[0].trim();
            if (property.isEmpty()) {
                continue;
            }
            Sort.Direction direction = Sort.Direction.ASC;
            if (parts.length > 1 && parts[1].trim().equalsIgnoreCase("desc")) {
                direction = Sort.Direction.DESC;
            }
            orders.add(new Sort.Order(direction, property));
        }
        return orders.isEmpty() ? Sort.by(Sort.Direction.DESC, "id") : Sort.by(orders);
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
