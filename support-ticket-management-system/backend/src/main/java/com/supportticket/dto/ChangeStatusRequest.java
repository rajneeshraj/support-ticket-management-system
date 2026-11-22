package com.supportticket.dto;

import com.supportticket.model.TicketStatus;
import jakarta.validation.constraints.NotNull;

public record ChangeStatusRequest(
        @NotNull(message = "Status is required. Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED.")
        TicketStatus status) {
}
