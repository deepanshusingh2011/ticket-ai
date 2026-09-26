package com.example.ticketai.repository;

import com.example.ticketai.domain.Ticket;
import com.example.ticketai.domain.TicketStatus;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByStatus(TicketStatus status);

    Page<Ticket> findByStatus(TicketStatus status, Pageable pageable);

    @Query("select t from Ticket t where lower(t.title) like lower(concat('%', :kw, '%')) "
            + "or lower(t.description) like lower(concat('%', :kw, '%'))")
    List<Ticket> searchByKeyword(@Param("kw") String keyword);

    @Query("select t from Ticket t where lower(t.title) like lower(concat('%', :kw, '%')) "
            + "or lower(t.description) like lower(concat('%', :kw, '%'))")
    Page<Ticket> searchByKeyword(@Param("kw") String keyword, Pageable pageable);

    @Query("select t from Ticket t where t.status = :status and "
            + "(lower(t.title) like lower(concat('%', :kw, '%')) "
            + "or lower(t.description) like lower(concat('%', :kw, '%')))")
    Page<Ticket> searchByKeywordAndStatus(
            @Param("kw") String keyword, @Param("status") TicketStatus status, Pageable pageable);
}
