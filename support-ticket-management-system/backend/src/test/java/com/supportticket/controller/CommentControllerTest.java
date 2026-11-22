package com.supportticket.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

class CommentControllerTest extends ApiTestBase {

    private ResultActions postComment(long ticketId, Map<String, Object> body) throws Exception {
        return mockMvc.perform(post("/api/v1/tickets/" + ticketId + "/comments")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    @Test
    void addCommentReturns201WithAuthorTextAndTime() throws Exception {
        long id = createTicket("T", "D");

        postComment(id, Map.of("author", "Asha", "text", "Replaced the toner."))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.author", is("Asha")))
                .andExpect(jsonPath("$.text", is("Replaced the toner.")))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void commentsAppearOnTheTicketOldestFirst() throws Exception {
        long id = createTicket("T", "D");
        postComment(id, Map.of("author", "A", "text", "first")).andExpect(status().isCreated());
        postComment(id, Map.of("author", "B", "text", "second")).andExpect(status().isCreated());

        assertThat(getTicket(id).get("comments").get(0).get("text").asText()).isEqualTo("first");
        assertThat(getTicket(id).get("comments").get(1).get("text").asText()).isEqualTo("second");
    }

    @Test
    void aClosedTicketStillAcceptsComments() throws Exception {
        long id = saveTicket(TicketStatus.CLOSED);

        postComment(id, Map.of("author", "Asha", "text", "Follow-up note"))
                .andExpect(status().isCreated());
    }

    @Test
    void blankTextIsRejectedAndNothingIsAdded() throws Exception {
        long id = createTicket("T", "D");

        postComment(id, Map.of("author", "Asha", "text", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("text")));

        assertThat(getTicket(id).get("comments")).isEmpty();
    }

    @Test
    void overLengthTextAndBlankAuthorAreRejectedTogether() throws Exception {
        long id = createTicket("T", "D");

        postComment(id, Map.of("author", " ", "text", "x".repeat(2001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors", hasSize(2)));
    }

    @Test
    void commentOnAnUnknownTicketIsNotFound() throws Exception {
        postComment(999_999L, Map.of("author", "A", "text", "text"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code", is("TICKET_NOT_FOUND")));
    }
}
