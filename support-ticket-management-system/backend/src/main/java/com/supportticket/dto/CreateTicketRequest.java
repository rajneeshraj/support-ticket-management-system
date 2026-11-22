package com.supportticket.dto;

import com.supportticket.model.TicketPriority;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Any extra property, such as {@code status}, is ignored: new tickets are always OPEN. */
public record CreateTicketRequest(
        @NotBlank(message = "Title is required and must not be blank.")
        @Size(max = 150, message = "Title must be at most 150 characters.")
        String title,

        @NotBlank(message = "Description is required and must not be blank.")
        @Size(max = 5000, message = "Description must be at most 5,000 characters.")
        String description,

        TicketPriority priority) {
}
