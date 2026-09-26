package com.example.ticketai.repository;

import com.example.ticketai.domain.Ticket;
import com.example.ticketai.domain.TicketStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    @Query("select t from Ticket t where lower(t.title) like lower(concat('%', :kw, '%')) "
            + "or lower(t.description) like lower(concat('%', :kw, '%'))")
    List<Ticket> searchByKeyword(@Param("kw") String keyword);
}
