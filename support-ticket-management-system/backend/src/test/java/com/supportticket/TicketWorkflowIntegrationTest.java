package com.supportticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.supportticket.support.ApiTestBase;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/** End-to-end walk through quickstart scenarios 1 to 6 with the real context, migrations and H2. */
class TicketWorkflowIntegrationTest extends ApiTestBase {

    @Test
    void fullTicketLifecycleWithEditsCommentsSearchAndRejections() throws Exception {
        // Scenario 1: create (supplied status ignored), list, view
        MvcResult created = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of(
                                "title", "Printer down",
                                "description", "Floor 2 printer offline",
                                "priority", "HIGH",
                                "status", "CLOSED"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andReturn();
        long id = read(created).get("id").asLong();

        mockMvc.perform(get("/api/v1/tickets"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id", is((int) id)));
        assertThat(getTicket(id).get("title").asText()).isEqualTo("Printer down");
        mockMvc.perform(get("/api/v1/tickets/999999999")).andExpect(status().isNotFound());

        // Scenario 4: update with the loaded version; stale version is refused
        long version = getTicket(id).get("version").asLong();
        putTicket(id, Map.of(
                        "title", "Printer on floor 2 down",
                        "description", "Floor 2 printer offline",
                        "priority", "CRITICAL",
                        "assignee", "Asha",
                        "version", version))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assignee", is("Asha")));
        putTicket(id, Map.of(
                        "title", "Stale edit",
                        "description", "d",
                        "priority", "LOW",
                        "version", version))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONCURRENT_MODIFICATION")));

        // Scenario 5: comments
        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("author", "Asha", "text", "Replaced the toner."))))
                .andExpect(status().isCreated());
        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("author", "Asha", "text", "   "))))
                .andExpect(status().isBadRequest());

        // Scenario 2 and 3: lifecycle, then rejections from the final status
        patchStatus(id, "RESOLVED").andExpect(status().isConflict());
        for (String next : List.of("IN_PROGRESS", "RESOLVED", "CLOSED")) {
            patchStatus(id, next).andExpect(status().isOk()).andExpect(jsonPath("$.status", is(next)));
        }
        JsonNode closed = getTicket(id);
        assertThat(closed.get("allowedNextStatuses")).isEmpty();
        assertThat(closed.get("comments")).hasSize(1);
        patchStatus(id, "OPEN")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_STATUS_TRANSITION")));
        assertThat(getTicket(id).get("updatedAt")).isEqualTo(closed.get("updatedAt"));

        // Final ticket still accepts a comment
        mockMvc.perform(post("/api/v1/tickets/" + id + "/comments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("author", "Ben", "text", "Confirmed fixed."))))
                .andExpect(status().isCreated());

        // Cancellation of a second ticket
        long second = createTicket("Cancel me", "Duplicate request");
        patchStatus(second, "CANCELLED").andExpect(status().isOk());

        // Scenario 6: search and filter
        mockMvc.perform(get("/api/v1/tickets?q=PRINTER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));
        mockMvc.perform(get("/api/v1/tickets?status=CANCELLED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].id", is((int) second)));
        mockMvc.perform(get("/api/v1/tickets?status=CLOSED&q=printer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));
        mockMvc.perform(get("/api/v1/tickets?status=OPEN&q=printer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }
}
