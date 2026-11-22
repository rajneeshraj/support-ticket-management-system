package com.supportticket.dto;

import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateTicketRequest(
        @NotBlank(message = "Title is required and must not be blank.")
        @Size(max = 150, message = "Title must be at most 150 characters.")
        String title,

        @NotBlank(message = "Description is required and must not be blank.")
        @Size(max = 5000, message = "Description must be at most 5,000 characters.")
        String description,

        @NotNull(message = "Priority is required. Valid values: LOW, MEDIUM, HIGH, CRITICAL.")
        TicketPriority priority,

        @Size(max = 100, message = "Assignee must be at most 100 characters.")
        String assignee,

        TicketStatus status,

        @NotNull(message = "Version is required. Reload the ticket and try again.")
        Long version) {
}
