package com.supportticket.dto;

import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import java.time.Instant;

/** List row. Loaded through a constructor projection so comments are never fetched. */
public record TicketSummary(
        Long id,
        String title,
        TicketStatus status,
        TicketPriority priority,
        String assignee,
        Instant createdAt) {
}
