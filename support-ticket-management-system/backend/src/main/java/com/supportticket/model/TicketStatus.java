package com.supportticket.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Ticket lifecycle states and the only allowed transitions between them.
 *
 * <pre>
 * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
 * OPEN -> CANCELLED
 * IN_PROGRESS -> CANCELLED
 * </pre>
 *
 * CLOSED and CANCELLED are final.
 */
public enum TicketStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED,
    CANCELLED;

    private static final Map<TicketStatus, List<TicketStatus>> ALLOWED_NEXT = new EnumMap<>(TicketStatus.class);

    static {
        ALLOWED_NEXT.put(OPEN, List.of(IN_PROGRESS, CANCELLED));
        ALLOWED_NEXT.put(IN_PROGRESS, List.of(RESOLVED, CANCELLED));
        ALLOWED_NEXT.put(RESOLVED, List.of(CLOSED));
        ALLOWED_NEXT.put(CLOSED, List.of());
        ALLOWED_NEXT.put(CANCELLED, List.of());
    }

    /** Statuses this status may move to, in display order. Empty for final statuses. */
    public List<TicketStatus> allowedNext() {
        return ALLOWED_NEXT.get(this);
    }

    /** True only for the five transitions in the workflow; same-status is not a transition. */
    public boolean canTransitionTo(TicketStatus target) {
        return allowedNext().contains(target);
    }

    public boolean isFinal() {
        return allowedNext().isEmpty();
    }
}
