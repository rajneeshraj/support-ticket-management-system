package com.supportticket.exception;

import com.supportticket.model.TicketStatus;
import java.util.List;
import java.util.stream.Collectors;

/** Thrown when a status change is not part of the allowed workflow. */
public class InvalidStatusTransitionException extends RuntimeException {

    private final TicketStatus currentStatus;
    private final TicketStatus requestedStatus;

    public InvalidStatusTransitionException(TicketStatus currentStatus, TicketStatus requestedStatus) {
        super(buildMessage(currentStatus, requestedStatus));
        this.currentStatus = currentStatus;
        this.requestedStatus = requestedStatus;
    }

    public TicketStatus getCurrentStatus() {
        return currentStatus;
    }

    public TicketStatus getRequestedStatus() {
        return requestedStatus;
    }

    public List<TicketStatus> getAllowedNextStatuses() {
        return currentStatus.allowedNext();
    }

    private static String buildMessage(TicketStatus current, TicketStatus requested) {
        String base = "Cannot change status from " + current + " to " + requested + ".";
        if (current.isFinal()) {
            return base + " " + current + " is a final status.";
        }
        String allowed = current.allowedNext().stream().map(Enum::name).collect(Collectors.joining(", "));
        return base + " Allowed next statuses: " + allowed + ".";
    }
}
