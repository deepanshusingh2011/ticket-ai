package com.example.ticketai;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.ticketai.domain.Priority;
import com.example.ticketai.domain.Ticket;
import com.example.ticketai.repository.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class TicketStateMachineTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TicketRepository tickets;

    private Long createOpenTicket() {
        Ticket ticket = tickets.saveAndFlush(
                new Ticket("Login broken", "Cannot log in", Priority.HIGH, "ada"));
        return ticket.getId();
    }

    private void moveTo(Long id, String status) throws Exception {
        mvc.perform(patch("/api/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"" + status + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(status));
    }

    @Test
    void validLifecycle_open_to_closed() throws Exception {
        Long id = createOpenTicket();
        moveTo(id, "IN_PROGRESS");
        moveTo(id, "RESOLVED");
        moveTo(id, "CLOSED");
    }

    @Test
    void valid_open_to_cancelled() throws Exception {
        Long id = createOpenTicket();
        moveTo(id, "CANCELLED");
    }

    @Test
    void valid_inProgress_to_cancelled() throws Exception {
        Long id = createOpenTicket();
        moveTo(id, "IN_PROGRESS");
        moveTo(id, "CANCELLED");
    }

    @Test
    void invalid_open_to_resolved_isRejected() throws Exception {
        Long id = createOpenTicket();
        mvc.perform(patch("/api/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"RESOLVED\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invalid_closed_to_open_isRejected() throws Exception {
        Long id = createOpenTicket();
        moveTo(id, "IN_PROGRESS");
        moveTo(id, "RESOLVED");
        moveTo(id, "CLOSED");
        mvc.perform(patch("/api/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invalid_resolved_to_open_isRejected() throws Exception {
        Long id = createOpenTicket();
        moveTo(id, "IN_PROGRESS");
        moveTo(id, "RESOLVED");
        mvc.perform(patch("/api/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void invalid_cancelled_to_open_isRejected() throws Exception {
        Long id = createOpenTicket();
        moveTo(id, "CANCELLED");
        mvc.perform(patch("/api/tickets/{id}/status", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"OPEN\"}"))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void validation_create_requiresTitleAndDescription() throws Exception {
        mvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"description\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.title").exists())
                .andExpect(jsonPath("$.fields.description").exists());
    }

    @Test
    void comment_requiresBody() throws Exception {
        Long id = createOpenTicket();
        mvc.perform(post("/api/tickets/{id}/comments", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"\"}"))
                .andExpect(status().isBadRequest());
    }
}
