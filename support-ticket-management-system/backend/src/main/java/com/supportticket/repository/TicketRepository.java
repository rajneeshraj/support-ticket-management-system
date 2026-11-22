package com.supportticket.repository;

import com.supportticket.dto.TicketSummary;
import com.supportticket.model.Ticket;
import com.supportticket.model.TicketStatus;
import java.time.Instant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    /**
     * Newest-first page of ticket summaries, optionally filtered by status and by a
     * pre-escaped, lower-cased LIKE pattern (escape character {@code !}). Only the list
     * columns are read, so comments are never loaded.
     */
    @Query(value = """
            select new com.supportticket.dto.TicketSummary(
                t.id, t.title, t.status, t.priority, t.assignee, t.createdAt)
            from Ticket t
            where (:status is null or t.status = :status)
              and (:pattern is null
                   or lower(t.title) like :pattern escape '!'
                   or lower(t.description) like :pattern escape '!')
            order by t.createdAt desc, t.id desc
            """,
            countQuery = """
            select count(t) from Ticket t
            where (:status is null or t.status = :status)
              and (:pattern is null
                   or lower(t.title) like :pattern escape '!'
                   or lower(t.description) like :pattern escape '!')
            """)
    Page<TicketSummary> search(
            @Param("status") TicketStatus status, @Param("pattern") String pattern, Pageable pageable);

    /**
     * Atomically moves a ticket to {@code to} only if it is still in {@code from}.
     *
     * @return 1 if the transition was applied, 0 if the status had changed in the meantime
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Ticket t
               set t.status = :to, t.updatedAt = :now, t.version = t.version + 1
             where t.id = :id and t.status = :from
            """)
    int updateStatusIfCurrent(
            @Param("id") Long id,
            @Param("from") TicketStatus from,
            @Param("to") TicketStatus to,
            @Param("now") Instant now);
}
