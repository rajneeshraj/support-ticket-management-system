package com.supportticket.dto;

import com.supportticket.model.Comment;
import com.supportticket.model.Ticket;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import java.time.Instant;
import java.util.List;

public record TicketDetail(
        Long id,
        String title,
        String description,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        Instant createdAt,
        Instant updatedAt,
        long version,
        List<TicketStatus> allowedNextStatuses,
        List<CommentResponse> comments) {

    public static TicketDetail from(Ticket ticket, List<Comment> comments) {
        return new TicketDetail(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getStatus(),
                ticket.getPriority(),
                ticket.getAssignee(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getVersion(),
                ticket.getStatus().allowedNext(),
                comments.stream().map(CommentResponse::from).toList());
    }
}
