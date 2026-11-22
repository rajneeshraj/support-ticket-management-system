package com.supportticket.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TicketSearchControllerTest extends ApiTestBase {

    @Test
    void keywordMatchesTitleOrDescriptionCaseInsensitively() throws Exception {
        saveTicket("Printer down", "Floor 2", TicketStatus.OPEN);
        saveTicket("Login broken", "Also affects the PRINTER queue", TicketStatus.OPEN);
        saveTicket("Unrelated", "Nothing", TicketStatus.OPEN);

        mockMvc.perform(get("/api/v1/tickets?q=PrInTeR"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(2)))
                .andExpect(jsonPath("$.content", hasSize(2)));
    }

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void statusFilterWorksForEveryStatusIncludingCancelled(TicketStatus status) throws Exception {
        for (TicketStatus s : TicketStatus.values()) {
            saveTicket("ticket " + s, "d", s);
        }

        mockMvc.perform(get("/api/v1/tickets?status=" + status.name()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].status", is(status.name())));
    }

    @Test
    void keywordAndStatusCombine() throws Exception {
        saveTicket("Printer open", "d", TicketStatus.OPEN);
        saveTicket("Printer closed", "d", TicketStatus.CLOSED);
        saveTicket("Other open", "d", TicketStatus.OPEN);

        mockMvc.perform(get("/api/v1/tickets?status=OPEN&q=printer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("Printer open")));
    }

    @Test
    void specialCharactersAreTreatedAsPlainText() throws Exception {
        saveTicket("100% done", "d", TicketStatus.OPEN);
        saveTicket("1000 done", "d", TicketStatus.OPEN);
        saveTicket("snake_case", "d", TicketStatus.OPEN);
        saveTicket("snakeXcase", "d", TicketStatus.OPEN);

        mockMvc.perform(get("/api/v1/tickets").param("q", "%"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("100% done")));
        mockMvc.perform(get("/api/v1/tickets").param("q", "_"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.content[0].title", is("snake_case")));
        mockMvc.perform(get("/api/v1/tickets").param("q", "<script>'; drop table ticket;--"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void blankKeywordIsTheSameAsNoKeyword() throws Exception {
        saveTicket(TicketStatus.OPEN);

        mockMvc.perform(get("/api/v1/tickets").param("q", "   "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", is(1)));
    }

    @Test
    void noMatchesReturnsAnEmptyPage() throws Exception {
        saveTicket(TicketStatus.OPEN);

        mockMvc.perform(get("/api/v1/tickets?q=zzz"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.totalElements", is(0)));
    }

    @Test
    void anUnknownStatusIsAValidationErrorListingValidValues() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?status=DONE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("status")))
                .andExpect(jsonPath("$.fieldErrors[0].message",
                        is("Unknown status 'DONE'. Valid values: OPEN, IN_PROGRESS, RESOLVED, CLOSED, CANCELLED.")));
    }

    @Test
    void keywordOverOneHundredCharactersIsRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tickets").param("q", "x".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_ERROR")))
                .andExpect(jsonPath("$.fieldErrors[0].field", is("q")));
    }

    @Test
    void sizeOverOneHundredOrBelowOneAndNegativePageAreRejected() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?size=101"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("size")));
        mockMvc.perform(get("/api/v1/tickets?size=0")).andExpect(status().isBadRequest());
        mockMvc.perform(get("/api/v1/tickets?page=-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field", is("page")));
    }

    @Test
    void sizeOfExactlyOneHundredIsAccepted() throws Exception {
        mockMvc.perform(get("/api/v1/tickets?size=100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size", is(100)));
    }
}
