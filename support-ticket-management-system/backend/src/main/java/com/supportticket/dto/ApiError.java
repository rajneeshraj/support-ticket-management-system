package com.supportticket.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.supportticket.model.TicketStatus;
import java.time.Instant;
import java.util.List;

/** The single error shape returned by every failing request. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
        Instant timestamp,
        int status,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors,
        TicketStatus currentStatus,
        TicketStatus requestedStatus,
        List<TicketStatus> allowedNextStatuses) {

    public static ApiError of(int status, String code, String message, String path, List<FieldError> fieldErrors) {
        return new ApiError(Instant.now(), status, code, message, path, fieldErrors, null, null, null);
    }
}
