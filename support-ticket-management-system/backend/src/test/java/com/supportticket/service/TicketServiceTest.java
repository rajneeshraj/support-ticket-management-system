package com.supportticket.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportticket.dto.CreateTicketRequest;
import com.supportticket.dto.TicketDetail;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TicketServiceTest extends ApiTestBase {

    @Autowired
    private TicketService ticketService;

    @Test
    void createStartsOpenWithNoAssigneeAndMatchingTimestamps() {
        TicketDetail created = ticketService.create(new CreateTicketRequest("Printer down", "Floor 2", TicketPriority.HIGH));

        assertThat(created.id()).isNotNull();
        assertThat(created.status()).isEqualTo(TicketStatus.OPEN);
        assertThat(created.assignee()).isNull();
        assertThat(created.priority()).isEqualTo(TicketPriority.HIGH);
        assertThat(created.createdAt()).isEqualTo(created.updatedAt());
        assertThat(created.version()).isZero();
        assertThat(created.allowedNextStatuses()).containsExactly(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
        assertThat(created.comments()).isEmpty();
    }

    @Test
    void createDefaultsPriorityToMedium() {
        TicketDetail created = ticketService.create(new CreateTicketRequest("Title", "Description", null));

        assertThat(created.priority()).isEqualTo(TicketPriority.MEDIUM);
    }

    @Test
    void createTrimsTitleAndDescription() {
        TicketDetail created = ticketService.create(new CreateTicketRequest("  Title  ", "  Description  ", null));

        assertThat(created.title()).isEqualTo("Title");
        assertThat(created.description()).isEqualTo("Description");
    }

    @Test
    void getReturnsTheTicketAndMissingTicketIsNotFound() {
        TicketDetail created = ticketService.create(new CreateTicketRequest("Title", "Description", null));

        assertThat(ticketService.get(created.id()).title()).isEqualTo("Title");
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> ticketService.get(999_999L))
                .isInstanceOf(TicketNotFoundException.class)
                .hasMessageContaining("999999");
    }
}
