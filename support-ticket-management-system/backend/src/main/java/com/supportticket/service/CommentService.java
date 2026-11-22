package com.supportticket.service;

import com.supportticket.dto.CommentResponse;
import com.supportticket.dto.CreateCommentRequest;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.model.Comment;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CommentService {

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    public CommentService(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    /** Adds a comment to any ticket, whatever its status. The ticket itself is not modified. */
    @Transactional
    public CommentResponse add(long ticketId, CreateCommentRequest request) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new TicketNotFoundException(ticketId);
        }
        Comment comment = new Comment(
                ticketRepository.getReferenceById(ticketId),
                request.author().strip(),
                request.text().strip(),
                Instant.now().truncatedTo(ChronoUnit.MICROS));
        return CommentResponse.from(commentRepository.save(comment));
    }
}
