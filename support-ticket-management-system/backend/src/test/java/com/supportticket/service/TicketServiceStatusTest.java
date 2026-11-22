package com.supportticket.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportticket.dto.TicketDetail;
import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;

class TicketServiceStatusTest extends ApiTestBase {

    @Autowired
    private TicketService ticketService;

    @ParameterizedTest(name = "{0} -> {1} is accepted")
    @CsvSource({
        "OPEN, IN_PROGRESS",
        "IN_PROGRESS, RESOLVED",
        "RESOLVED, CLOSED",
        "OPEN, CANCELLED",
        "IN_PROGRESS, CANCELLED"
    })
    void eachAllowedTransitionIsAcceptedAndRefreshesTimestampAndVersion(TicketStatus from, TicketStatus to) {
        long id = saveTicket(from);
        TicketDetail before = ticketService.get(id);

        TicketDetail after = ticketService.changeStatus(id, to);

        assertThat(after.status()).isEqualTo(to);
        assertThat(after.updatedAt()).isAfter(before.updatedAt());
        assertThat(after.version()).isGreaterThan(before.version());
        assertThat(after.allowedNextStatuses()).isEqualTo(to.allowedNext());
        assertThat(ticketService.get(id).status()).isEqualTo(to);
    }

    @Test
    void theFullLifecycleCanBeWalkedFromOpenToClosed() {
        long id = saveTicket(TicketStatus.OPEN);

        List<TicketStatus> path = List.of(TicketStatus.IN_PROGRESS, TicketStatus.RESOLVED, TicketStatus.CLOSED);
        path.forEach(next -> assertThat(ticketService.changeStatus(id, next).status()).isEqualTo(next));

        assertThat(ticketService.get(id).allowedNextStatuses()).isEmpty();
    }

    @Test
    void sameStatusIsANoOpThatLeavesTheTicketUntouched() {
        long id = saveTicket(TicketStatus.OPEN);
        TicketDetail before = ticketService.get(id);

        TicketDetail after = ticketService.changeStatus(id, TicketStatus.OPEN);

        assertThat(after.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(after.version()).isEqualTo(before.version());
        assertThat(after.updatedAt()).isEqualTo(before.updatedAt());
    }
}
