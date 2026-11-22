package com.supportticket.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportticket.support.ApiTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class TicketControllerTest extends ApiTestBase {

    @Test
    void createReturns201WithLocationAndAnOpenTicketIgnoringASuppliedStatus() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Printer down",
                                "description", "Floor 2 printer offline",
                                "priority", "HIGH",
                                "status", "CLOSED"))))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern("/api/v1/tickets/\\d+")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.priority", is("HIGH")))
                .andExpect(jsonPath("$.assignee").doesNotExist())
                .andExpect(jsonPath("$.allowedNextStatuses[0]", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.allowedNextStatuses[1]", is("CANCELLED")))
                .andExpect(jsonPath("$.comments", hasSize(0)));
    }

    @Test
    void createWithoutPriorityDefaultsToMedium() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "T", "description", "D"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.priority", is("MEDIUM")));
    }

    @Test
    void createRejectsBlankAndOverLengthFieldsWithFieldErrorsAndSavesNothing() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "   ", "description", "x".repeat(5001)))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.status", is(400)))
                .andExpect(jsonPath("$.path", is("/api/v1/tickets")))
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='title')]").exists())
                .andExpect(jsonPath("$.fieldErrors[?(@.field=='description')]").exists());

        org.assertj.core.api.Assertions.assertThat(ticketRepository.count()).isZero();
    }

    @Test
    void createRejectsTitleLongerThan150Characters() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "t".repeat(151), "description", "D"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("title")))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString("150")));
    }

    @Test
    void createRejectsAnUnknownPriorityListingTheValidValues() throws Exception {
        mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", "T", "description", "D", "priority", "URGENT"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("priority")))
                .andExpect(jsonPath("$.fieldErrors[0].message", containsString("LOW, MEDIUM, HIGH, CRITICAL")));
    }

    @Test
    void listReturnsNewestFirstInPagesWithTotals() throws Exception {
        createTicket("first", "d");
        Thread.sleep(5);
        createTicket("second", "d");

        mockMvc.perform(get("/api/v1/tickets?size=1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].title", is("second")))
                .andExpect(jsonPath("$.page", is(0)))
                .andExpect(jsonPath("$.size", is(1)))
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.totalPages", is(2)));
    }

    @Test
    void listIsEmptyWhenThereAreNoTickets() throws Exception {
        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void getReturnsFullDetails() throws Exception {
        long id = createTicket("Title", "Description");

        mockMvc.perform(get("/api/v1/tickets/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is((int) id)))
                .andExpect(jsonPath("$.title", is("Title")))
                .andExpect(jsonPath("$.description", is("Description")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists())
                .andExpect(jsonPath("$.version", is(0)))
                .andExpect(jsonPath("$.comments", hasSize(0)));
    }

    @Test
    void getUnknownTicketIsNotFoundWithAPlainMessage() throws Exception {
        mockMvc.perform(get("/api/v1/tickets/999999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TICKET_NOT_FOUND")))
                .andExpect(jsonPath("$.message", is("Ticket 999999999 was not found.")))
                .andExpect(jsonPath("$.path", is("/api/v1/tickets/999999999")));
    }
}
