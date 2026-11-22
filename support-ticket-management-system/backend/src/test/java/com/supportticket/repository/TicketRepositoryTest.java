package com.supportticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportticket.dto.TicketSummary;
import com.supportticket.model.Ticket;
import com.supportticket.model.TicketPriority;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TicketRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TicketRepository ticketRepository;

    private Ticket save(String title, Instant createdAt) {
        return ticketRepository.save(new Ticket(title, "Description of " + title, TicketPriority.MEDIUM, createdAt));
    }

    @Test
    void listIsNewestFirstWithIdAsTieBreaker() {
        save("oldest", BASE);
        Ticket first = save("same-time-first", BASE.plus(1, ChronoUnit.HOURS));
        Ticket second = save("same-time-second", BASE.plus(1, ChronoUnit.HOURS));
        save("newest", BASE.plus(2, ChronoUnit.HOURS));

        Page<TicketSummary> page = ticketRepository.search(null, null, PageRequest.of(0, 10));

        assertThat(page.getContent()).extracting(TicketSummary::title)
                .containsExactly("newest", "same-time-second", "same-time-first", "oldest");
        assertThat(second.getId()).isGreaterThan(first.getId());
    }

    @Test
    void listIsPagedAndReportsTotals() {
        for (int i = 0; i < 5; i++) {
            save("ticket-" + i, BASE.plus(i, ChronoUnit.MINUTES));
        }

        Page<TicketSummary> secondPage = ticketRepository.search(null, null, PageRequest.of(1, 2));

        assertThat(secondPage.getContent()).hasSize(2);
        assertThat(secondPage.getTotalElements()).isEqualTo(5);
        assertThat(secondPage.getTotalPages()).isEqualTo(3);
        assertThat(secondPage.getContent()).extracting(TicketSummary::title)
                .containsExactly("ticket-2", "ticket-1");
    }

    @Test
    void listReturnsSummaryColumnsOnly() {
        save("only", BASE);

        TicketSummary summary = ticketRepository.search(null, null, PageRequest.of(0, 10)).getContent().get(0);

        assertThat(summary.id()).isNotNull();
        assertThat(summary.title()).isEqualTo("only");
        assertThat(summary.status()).isNotNull();
        assertThat(summary.priority()).isEqualTo(TicketPriority.MEDIUM);
        assertThat(summary.assignee()).isNull();
        assertThat(summary.createdAt()).isEqualTo(BASE);
    }
}
