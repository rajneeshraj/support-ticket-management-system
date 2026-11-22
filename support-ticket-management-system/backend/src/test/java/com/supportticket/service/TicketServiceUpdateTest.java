package com.supportticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.supportticket.dto.TicketDetail;
import com.supportticket.dto.UpdateTicketRequest;
import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.model.TicketPriority;
import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

class TicketServiceUpdateTest extends ApiTestBase {

    @Autowired
    private TicketService ticketService;

    private UpdateTicketRequest request(long version, String assignee, TicketStatus status) {
        return new UpdateTicketRequest("Updated title", "Updated description", TicketPriority.CRITICAL, assignee, status, version);
    }

    @Test
    void updateChangesFieldsStoresBlankAssigneeAsEmptyAndRefreshesTimestampAndVersion() {
        long id = saveTicket(TicketStatus.OPEN);
        TicketDetail before = ticketService.get(id);

        TicketDetail after = ticketService.update(id, request(before.version(), "   ", null));

        assertThat(after.title()).isEqualTo("Updated title");
        assertThat(after.description()).isEqualTo("Updated description");
        assertThat(after.priority()).isEqualTo(TicketPriority.CRITICAL);
        assertThat(after.assignee()).isNull();
        assertThat(after.updatedAt()).isAfter(before.updatedAt());
        assertThat(after.version()).isGreaterThan(before.version());
        assertThat(after.status()).isEqualTo(TicketStatus.OPEN);
    }

    @Test
    void updateAppliesAnAllowedStatusChange() {
        long id = saveTicket(TicketStatus.OPEN);

        TicketDetail after = ticketService.update(id, request(0, "Asha", TicketStatus.CANCELLED));

        assertThat(after.status()).isEqualTo(TicketStatus.CANCELLED);
        assertThat(after.assignee()).isEqualTo("Asha");
    }

    @Test
    void aRejectedStatusRollsBackEveryFieldChange() {
        long id = saveTicket("Original", "Original description", TicketStatus.RESOLVED);

        assertThatThrownBy(() -> ticketService.update(id, request(0, "Asha", TicketStatus.OPEN)))
                .isInstanceOf(InvalidStatusTransitionException.class);

        TicketDetail unchanged = ticketService.get(id);
        assertThat(unchanged.title()).isEqualTo("Original");
        assertThat(unchanged.assignee()).isNull();
        assertThat(unchanged.status()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(unchanged.version()).isZero();
    }

    @Test
    void aStaleVersionIsRejectedAndNothingChanges() {
        long id = saveTicket("Original", "Original description", TicketStatus.OPEN);
        ticketService.update(id, request(0, null, null));

        assertThatThrownBy(() -> ticketService.update(id, request(0, null, null)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(ticketService.get(id).version()).isEqualTo(1);
    }

    @Test
    void aStatusChangeMadeByAnotherUserAlsoInvalidatesAStaleUpdate() {
        long id = saveTicket(TicketStatus.OPEN);
        ticketService.changeStatus(id, TicketStatus.IN_PROGRESS);

        assertThatThrownBy(() -> ticketService.update(id, request(0, null, null)))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        assertThat(ticketService.get(id).status()).isEqualTo(TicketStatus.IN_PROGRESS);
    }
}
