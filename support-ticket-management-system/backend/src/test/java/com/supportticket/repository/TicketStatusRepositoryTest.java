package com.supportticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportticket.model.Ticket;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class TicketStatusRepositoryTest {

    private static final Instant T0 = Instant.parse("2026-01-01T00:00:00Z");
    private static final Instant T1 = Instant.parse("2026-01-02T00:00:00Z");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private EntityManager entityManager;

    private Long saveOpenTicket() {
        Ticket ticket = ticketRepository.saveAndFlush(new Ticket("Title", "Description", TicketPriority.LOW, T0));
        entityManager.clear();
        return ticket.getId();
    }

    @Test
    void conditionalUpdateChangesExactlyOneRowWhenTheStatusStillMatches() {
        Long id = saveOpenTicket();

        int rows = ticketRepository.updateStatusIfCurrent(id, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, T1);

        assertThat(rows).isEqualTo(1);
        Ticket reloaded = ticketRepository.findById(id).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(reloaded.getUpdatedAt()).isEqualTo(T1);
        assertThat(reloaded.getVersion()).isEqualTo(1);
    }

    @Test
    void conditionalUpdateChangesNothingWhenTheStatusHasMovedOn() {
        Long id = saveOpenTicket();
        ticketRepository.updateStatusIfCurrent(id, TicketStatus.OPEN, TicketStatus.CANCELLED, T1);

        int rows = ticketRepository.updateStatusIfCurrent(id, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, T1);

        assertThat(rows).isZero();
        assertThat(ticketRepository.findById(id).orElseThrow().getStatus()).isEqualTo(TicketStatus.CANCELLED);
    }

    @Test
    void conditionalUpdateOfAMissingTicketChangesNothing() {
        int rows = ticketRepository.updateStatusIfCurrent(999_999L, TicketStatus.OPEN, TicketStatus.IN_PROGRESS, T1);

        assertThat(rows).isZero();
    }
}
