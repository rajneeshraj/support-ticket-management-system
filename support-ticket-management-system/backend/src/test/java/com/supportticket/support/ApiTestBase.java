package com.supportticket.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supportticket.model.Ticket;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import com.supportticket.repository.CommentRepository;
import com.supportticket.repository.TicketRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Full-stack test base: real Spring context, real migrations, in-memory H2, clean tables per test. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class ApiTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected TicketRepository ticketRepository;

    @Autowired
    protected CommentRepository commentRepository;

    @BeforeEach
    void cleanDatabase() {
        commentRepository.deleteAllInBatch();
        ticketRepository.deleteAllInBatch();
    }

    /** Creates a ticket through the API and returns its id. */
    protected long createTicket(String title, String description) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(Map.of("title", title, "description", description))))
                .andExpect(status().isCreated())
                .andReturn();
        return read(result).get("id").asLong();
    }

    /** Saves a ticket directly in the given status (test setup only; bypasses the workflow). */
    protected long saveTicket(TicketStatus status) {
        return saveTicket("Title", "Description", status);
    }

    protected long saveTicket(String title, String description, TicketStatus status) {
        Ticket ticket = new Ticket(title, description, TicketPriority.MEDIUM, nowMicros());
        ticket.moveTo(status);
        return ticketRepository.save(ticket).getId();
    }

    protected ResultActions patchStatus(long id, String status) throws Exception {
        return mockMvc.perform(patch("/api/v1/tickets/" + id + "/status")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("status", status))));
    }

    protected ResultActions putTicket(long id, Map<String, Object> body) throws Exception {
        return mockMvc.perform(put("/api/v1/tickets/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(body)));
    }

    protected JsonNode getTicket(long id) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/tickets/" + id)).andExpect(status().isOk()).andReturn();
        return read(result);
    }

    protected JsonNode read(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    protected String json(Object value) throws Exception {
        return objectMapper.writeValueAsString(value);
    }

    protected static Instant nowMicros() {
        return Instant.now().truncatedTo(ChronoUnit.MICROS);
    }
}
