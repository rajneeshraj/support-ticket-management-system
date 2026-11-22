package com.supportticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportticket.dto.TicketSummary;
import com.supportticket.model.Ticket;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

/** Patterns here are what TicketService hands to the repository: lower-cased, escaped, wrapped in %. */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TicketSearchRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TicketRepository ticketRepository;

    private int counter;

    @BeforeEach
    void resetCounter() {
        counter = 0;
    }

    private void save(String title, String description, TicketStatus status) {
        Ticket ticket = new Ticket(title, description, TicketPriority.MEDIUM, BASE.plus(counter++, ChronoUnit.MINUTES));
        ticket.moveTo(status);
        ticketRepository.save(ticket);
    }

    private List<String> titles(TicketStatus status, String pattern) {
        return ticketRepository.search(status, pattern, PageRequest.of(0, 50)).getContent().stream()
                .map(TicketSummary::title)
                .toList();
    }

    @Test
    void matchesOnTitleOrDescriptionIgnoringCase() {
        save("Printer down", "Floor 2", TicketStatus.OPEN);
        save("Login broken", "The PRINTER queue is also stuck", TicketStatus.OPEN);
        save("Unrelated", "Nothing here", TicketStatus.OPEN);

        assertThat(titles(null, "%printer%")).containsExactly("Login broken", "Printer down");
    }

    @Test
    void noKeywordReturnsEverything() {
        save("A", "a", TicketStatus.OPEN);
        save("B", "b", TicketStatus.CLOSED);

        assertThat(titles(null, null)).containsExactly("B", "A");
    }

    @Test
    void percentUnderscoreAndEscapeCharacterAreTreatedAsLiterals() {
        save("100% done", "d", TicketStatus.OPEN);
        save("1000 done", "d", TicketStatus.OPEN);
        save("snake_case", "d", TicketStatus.OPEN);
        save("snakeXcase", "d", TicketStatus.OPEN);
        save("wow!", "d", TicketStatus.OPEN);
        save("wow", "d", TicketStatus.OPEN);
        save("back\\slash", "d", TicketStatus.OPEN);

        assertThat(titles(null, "%100!%%")).containsExactly("100% done");
        assertThat(titles(null, "%snake!_case%")).containsExactly("snake_case");
        assertThat(titles(null, "%wow!!%")).containsExactly("wow!");
        assertThat(titles(null, "%back\\slash%")).containsExactly("back\\slash");
    }

    @Test
    void statusFilterReturnsOnlyThatStatusIncludingCancelled() {
        for (TicketStatus status : TicketStatus.values()) {
            save("ticket " + status, "d", status);
        }

        for (TicketStatus status : TicketStatus.values()) {
            assertThat(titles(status, null)).containsExactly("ticket " + status);
        }
    }

    @Test
    void statusAndKeywordTogetherNarrowTheResults() {
        save("Printer open", "d", TicketStatus.OPEN);
        save("Printer closed", "d", TicketStatus.CLOSED);
        save("Other open", "d", TicketStatus.OPEN);

        assertThat(titles(TicketStatus.OPEN, "%printer%")).containsExactly("Printer open");
    }

    @Test
    void resultsAreNewestFirstAndTotalsCountOnlyMatches() {
        save("match one", "d", TicketStatus.OPEN);
        save("skip", "d", TicketStatus.OPEN);
        save("match two", "d", TicketStatus.OPEN);

        var page = ticketRepository.search(null, "%match%", PageRequest.of(0, 1));

        assertThat(page.getContent()).extracting(TicketSummary::title).containsExactly("match two");
        assertThat(page.getTotalElements()).isEqualTo(2);
        assertThat(page.getTotalPages()).isEqualTo(2);
    }
}
