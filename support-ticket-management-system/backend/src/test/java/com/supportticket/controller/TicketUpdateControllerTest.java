package com.supportticket.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TicketUpdateControllerTest extends ApiTestBase {

    private Map<String, Object> body(long version) {
        Map<String, Object> body = new HashMap<>();
        body.put("title", "New title");
        body.put("description", "New description");
        body.put("priority", "LOW");
        body.put("assignee", "Asha");
        body.put("version", version);
        return body;
    }

    @Test
    void updateChangesTheFourFieldsRefreshesTimestampAndBumpsVersion() throws Exception {
        long id = createTicket("Old title", "Old description");
        JsonNode before = getTicket(id);

        putTicket(id, body(before.get("version").asLong()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("New title")))
                .andExpect(jsonPath("$.description", is("New description")))
                .andExpect(jsonPath("$.priority", is("LOW")))
                .andExpect(jsonPath("$.assignee", is("Asha")))
                .andExpect(jsonPath("$.status", is("OPEN")))
                .andExpect(jsonPath("$.version", is(1)));

        JsonNode after = getTicket(id);
        assertThat(after.get("updatedAt").asText()).isNotEqualTo(before.get("updatedAt").asText());
        assertThat(after.get("title").asText()).isEqualTo("New title");
    }

    @Test
    void blankAssigneeClearsTheAssignee() throws Exception {
        long id = createTicket("T", "D");
        putTicket(id, body(0)).andExpect(status().isOk());

        Map<String, Object> clear = body(1);
        clear.put("assignee", "  ");
        putTicket(id, clear).andExpect(status().isOk()).andExpect(jsonPath("$.assignee").doesNotExist());
    }

    @Test
    void blankTitleIsRejectedAndNothingChanges() throws Exception {
        long id = createTicket("Keep me", "Keep me too");
        Map<String, Object> bad = body(0);
        bad.put("title", "   ");

        putTicket(id, bad)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("title")));

        JsonNode after = getTicket(id);
        assertThat(after.get("title").asText()).isEqualTo("Keep me");
        assertThat(after.get("version").asLong()).isZero();
    }

    @Test
    void missingVersionIsRejected() throws Exception {
        long id = createTicket("T", "D");
        Map<String, Object> noVersion = body(0);
        noVersion.remove("version");

        putTicket(id, noVersion)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("version")));
    }

    @Test
    void staleVersionIsRejectedAsAConcurrentModificationAndNothingChanges() throws Exception {
        long id = createTicket("Original", "D");
        putTicket(id, body(0)).andExpect(status().isOk());

        Map<String, Object> stale = body(0);
        stale.put("title", "Overwrite attempt");
        putTicket(id, stale)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("CONCURRENT_MODIFICATION")))
                .andExpect(jsonPath("$.message", containsString("changed by someone else")));

        assertThat(getTicket(id).get("title").asText()).isEqualTo("New title");
    }

    @Test
    void anAllowedStatusInsideAnUpdateIsApplied() throws Exception {
        long id = createTicket("T", "D");
        Map<String, Object> withStatus = body(0);
        withStatus.put("status", "IN_PROGRESS");

        putTicket(id, withStatus)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.title", is("New title")));
    }

    @Test
    void aDisallowedStatusInsideAnUpdateIsRejectedAndNoFieldIsChanged() throws Exception {
        long id = saveTicket("Original", "Original description", TicketStatus.CLOSED);
        Map<String, Object> reopen = body(0);
        reopen.put("status", "OPEN");

        putTicket(id, reopen)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code", is("INVALID_STATUS_TRANSITION")))
                .andExpect(jsonPath("$.currentStatus", is("CLOSED")))
                .andExpect(jsonPath("$.requestedStatus", is("OPEN")));

        JsonNode after = getTicket(id);
        assertThat(after.get("status").asText()).isEqualTo("CLOSED");
        assertThat(after.get("title").asText()).isEqualTo("Original");
    }

    @Test
    void sameStatusInsideAnUpdateIsNotRejected() throws Exception {
        long id = createTicket("T", "D");
        Map<String, Object> same = body(0);
        same.put("status", "OPEN");

        putTicket(id, same).andExpect(status().isOk()).andExpect(jsonPath("$.status", is("OPEN")));
    }

    @Test
    void finalTicketsStillAllowFieldEditsWithoutAStatusChange() throws Exception {
        long id = saveTicket(TicketStatus.CLOSED);

        putTicket(id, body(0))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("CLOSED")))
                .andExpect(jsonPath("$.title", is("New title")));
    }

    @Test
    void updatingAnUnknownTicketIsNotFound() throws Exception {
        putTicket(999_999L, body(0))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TICKET_NOT_FOUND")));
    }
}
