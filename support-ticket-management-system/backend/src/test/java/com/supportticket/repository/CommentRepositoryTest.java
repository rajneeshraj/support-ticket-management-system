package com.supportticket.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportticket.model.Comment;
import com.supportticket.model.Ticket;
import com.supportticket.model.TicketPriority;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class CommentRepositoryTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private CommentRepository commentRepository;

    @Test
    void commentsForATicketComeBackOldestFirstAndOnlyForThatTicket() {
        Ticket ticket = ticketRepository.save(new Ticket("T", "D", TicketPriority.MEDIUM, BASE));
        Ticket other = ticketRepository.save(new Ticket("Other", "D", TicketPriority.MEDIUM, BASE));
        commentRepository.save(new Comment(ticket, "Asha", "third", BASE.plus(3, ChronoUnit.MINUTES)));
        commentRepository.save(new Comment(ticket, "Ben", "first", BASE.plus(1, ChronoUnit.MINUTES)));
        commentRepository.save(new Comment(ticket, "Asha", "second", BASE.plus(2, ChronoUnit.MINUTES)));
        commentRepository.save(new Comment(other, "Zed", "not mine", BASE));

        assertThat(commentRepository.findByTicketIdOldestFirst(ticket.getId()))
                .extracting(Comment::getText)
                .containsExactly("first", "second", "third");
    }

    @Test
    void commentsWithTheSameTimestampKeepInsertionOrder() {
        Ticket ticket = ticketRepository.save(new Ticket("T", "D", TicketPriority.MEDIUM, BASE));
        commentRepository.save(new Comment(ticket, "A", "one", BASE));
        commentRepository.save(new Comment(ticket, "A", "two", BASE));

        assertThat(commentRepository.findByTicketIdOldestFirst(ticket.getId()))
                .extracting(Comment::getText)
                .containsExactly("one", "two");
    }
}
