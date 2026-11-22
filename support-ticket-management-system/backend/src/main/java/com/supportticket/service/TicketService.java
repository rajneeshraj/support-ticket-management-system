package com.supportticket.service;

import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketDetail;
import com.supportticket.dto.TicketPage;
import com.supportticket.dto.UpdateTicketRequest;
import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.model.Ticket;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Business rules for tickets. This class is the only place that writes a ticket's status, so the
 * workflow in {@link TicketStatus} cannot be bypassed by any endpoint.
 */
@Service
public class TicketService {

    public static final int MAX_PAGE_SIZE = 100;
    private static final int MAX_STATUS_ATTEMPTS = 3;
    private static final char LIKE_ESCAPE = '!';

    private final TicketRepository ticketRepository;
    private final CommentRepository commentRepository;

    public TicketService(TicketRepository ticketRepository, CommentRepository commentRepository) {
        this.ticketRepository = ticketRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public TicketDetail create(CreateTicketRequest request) {
        TicketPriority priority = request.priority() != null ? request.priority() : TicketPriority.DEFAULT;
        Ticket ticket = new Ticket(request.title().strip(), request.description().strip(), priority, now());
        ticketRepository.save(ticket);
        return TicketDetail.from(ticket, List.of());
    }

    @Transactional(readOnly = true)
    public TicketPage list(TicketStatus status, String keyword, int page, int size) {
        PageRequest pageable = PageRequest.of(Math.max(0, page), Math.min(Math.max(1, size), MAX_PAGE_SIZE));
        return TicketPage.from(ticketRepository.search(status, toLikePattern(keyword), pageable));
    }

    @Transactional(readOnly = true)
    public TicketDetail get(long id) {
        return detail(findTicket(id));
    }

    /**
     * Applies one status change. Same-status requests are not transitions and return the ticket.
     * The write is a single conditional update, so simultaneous changes are applied one at a time
     * and each is judged against the status at that moment.
     */
    @Transactional
    public TicketDetail changeStatus(long id, TicketStatus target) {
        for (int attempt = 0; attempt < MAX_STATUS_ATTEMPTS; attempt++) {
            Ticket ticket = findTicket(id);
            TicketStatus current = ticket.getStatus();
            if (current == target) {
                return detail(ticket);
            }
            if (!current.canTransitionTo(target)) {
                throw new InvalidStatusTransitionException(current, target);
            }
            if (ticketRepository.updateStatusIfCurrent(id, current, target, now()) == 1) {
                return detail(findTicket(id));
            }
            // 0 rows: someone changed the status first. Reload and judge against the new status.
        }
        throw new ObjectOptimisticLockingFailureException(Ticket.class, id);
    }

    @Transactional
    public TicketDetail update(long id, UpdateTicketRequest request) {
        Ticket ticket = findTicket(id);
        if (request.version() != ticket.getVersion()) {
            throw new ObjectOptimisticLockingFailureException(Ticket.class, id);
        }
        TicketStatus requested = request.status();
        if (requested != null && requested != ticket.getStatus()) {
            if (!ticket.getStatus().canTransitionTo(requested)) {
                throw new InvalidStatusTransitionException(ticket.getStatus(), requested);
            }
            ticket.moveTo(requested);
        }
        ticket.edit(
                request.title().strip(),
                request.description().strip(),
                request.priority(),
                normalizeAssignee(request.assignee()),
                now());
        ticketRepository.saveAndFlush(ticket);
        return detail(ticket);
    }

    private Ticket findTicket(long id) {
        return ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
    }

    private TicketDetail detail(Ticket ticket) {
        return TicketDetail.from(ticket, commentRepository.findByTicketIdOldestFirst(ticket.getId()));
    }

    private static Instant now() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    private static String normalizeAssignee(String assignee) {
        return assignee == null || assignee.isBlank() ? null : assignee.strip();
    }

    /** Lower-cases the keyword and escapes LIKE wildcards so every character is plain text. */
    static String toLikePattern(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        String escaped = keyword.strip().toLowerCase(Locale.ROOT)
                .replace(String.valueOf(LIKE_ESCAPE), LIKE_ESCAPE + "" + LIKE_ESCAPE)
                .replace("%", LIKE_ESCAPE + "%")
                .replace("_", LIKE_ESCAPE + "_");
        return "%" + escaped + "%";
    }
}
