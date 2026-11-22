package com.supportticket.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import org.junit.jupiter.api.Test;

class TicketStatusControllerTest extends ApiTestBase {

    @Test
    void openTicketMovesToInProgressAndReportsTheNextAllowedStatuses() throws Exception {
        long id = createTicket("Title", "Description");

        patchStatus(id, "IN_PROGRESS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.allowedNextStatuses[0]", is("RESOLVED")))
                .andExpect(jsonPath("$.allowedNextStatuses[1]", is("CANCELLED")))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    void closedTicketReportsNoAllowedNextStatuses() throws Exception {
        long id = createTicket("Title", "Description");

        patchStatus(id, "IN_PROGRESS").andExpect(status().isOk());
        patchStatus(id, "RESOLVED").andExpect(status().isOk());
        patchStatus(id, "CLOSED")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")))
                .andExpect(jsonPath("$.allowedNextStatuses", hasSize(0)));
    }

    @Test
    void openAndInProgressTicketsCanBeCancelled() throws Exception {
        long open = saveTicket(TicketStatus.OPEN);
        long inProgress = saveTicket(TicketStatus.IN_PROGRESS);

        patchStatus(open, "CANCELLED").andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CANCELLED")));
        patchStatus(inProgress, "CANCELLED").andExpect(status().isOk()).andExpect(jsonPath("$.status", is("CANCELLED")));
    }

    @Test
    void sameStatusReturnsTheTicketWithoutError() throws Exception {
        long id = createTicket("Title", "Description");

        patchStatus(id, "OPEN")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.version", is(0)));
    }

    @Test
    void unknownTicketIsNotFound() throws Exception {
        patchStatus(999_999L, "IN_PROGRESS")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TICKET_NOT_FOUND")));
    }
}
