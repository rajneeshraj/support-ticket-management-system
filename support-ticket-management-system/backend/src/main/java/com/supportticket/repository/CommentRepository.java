package com.supportticket.repository;

import com.supportticket.model.Comment;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, Long> {

    /** Comments for one ticket, oldest first. */
    @Query("select c from Comment c where c.ticket.id = :ticketId order by c.createdAt asc, c.id asc")
    List<Comment> findByTicketIdOldestFirst(@Param("ticketId") Long ticketId);
}
