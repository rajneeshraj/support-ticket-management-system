package com.supportticket.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.MediaType;

class TicketStatusRejectionTest extends ApiTestBase {

    /** Every ordered pair of different statuses that is not one of the five allowed transitions. */
    static Stream<Arguments> rejectedPairs() {
        List<Arguments> rejected = new ArrayList<>();
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                if (from != to && !from.allowedNext().contains(to)) {
                    rejected.add(Arguments.of(from, to));
                }
            }
        }
        return rejected.stream();
    }

    @Test
    void thereAreFifteenRejectedPairs() {
        assertThat(rejectedPairs().count()).isEqualTo(15);
    }

    @ParameterizedTest(name = "{0} -> {1} is rejected with 409")
    @MethodSource("rejectedPairs")
    void rejectedTransitionReturns409NamingCurrentRequestedAndAllowedStatusesAndChangesNothing(
            TicketStatus from, TicketStatus to) throws Exception {
        long id = saveTicket(from);
        JsonNode before = getTicket(id);

        patchStatus(id, to.name())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status", is(409)))
                .andExpect(jsonPath("$.code", is("INVALID_STATUS_TRANSITION")))
                .andExpect(jsonPath("$.currentStatus", is(from.name())))
                .andExpect(jsonPath("$.requestedStatus", is(to.name())))
                .andExpect(jsonPath("$.allowedNextStatuses", hasSize(from.allowedNext().size())))
                .andExpect(jsonPath("$.path", is("/api/v1/tickets/" + id + "/status")))
                .andExpect(jsonPath("$.message", containsString(from.name())))
                .andExpect(jsonPath("$.message", containsString(to.name())));

        JsonNode after = getTicket(id);
        assertThat(after.get("status").asText()).isEqualTo(from.name());
        assertThat(after.get("updatedAt")).isEqualTo(before.get("updatedAt"));
        assertThat(after.get("version")).isEqualTo(before.get("version"));
    }

    @Test
    void messageSaysFinalForClosedAndCancelledTickets() throws Exception {
        long closed = saveTicket(TicketStatus.CLOSED);
        long cancelled = saveTicket(TicketStatus.CANCELLED);

        patchStatus(closed, "OPEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", is("Cannot change status from CLOSED to OPEN. CLOSED is a final status.")))
                .andExpect(jsonPath("$.allowedNextStatuses", hasSize(0)));
        patchStatus(cancelled, "OPEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message", containsString("CANCELLED is a final status")));
    }

    @Test
    void messageListsAllowedNextStatusesForNonFinalTickets() throws Exception {
        long id = saveTicket(TicketStatus.OPEN);

        patchStatus(id, "RESOLVED")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message",
                        is("Cannot change status from OPEN to RESOLVED. Allowed next statuses: IN_PROGRESS, CANCELLED.")));
    }

    @Test
    void unknownStatusValueIsAValidationErrorListingValidValuesNotATransitionError() throws Exception {
        long id = saveTicket(TicketStatus.OPEN);

        patchStatus(id, "DONE")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("status")))
                .andExpect(jsonPath("$.fieldErrors[0].message",
                        is("Unknown status 'DONE'. Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED.")));
    }

    @Test
    void missingStatusIsAValidationError() throws Exception {
        long id = saveTicket(TicketStatus.OPEN);

        mockMvc.perform(patch("/api/v1/tickets/" + id + "/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("status")));
    }
}
