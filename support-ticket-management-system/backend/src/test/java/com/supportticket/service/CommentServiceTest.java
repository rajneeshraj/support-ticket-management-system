package com.supportticket.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.supportticket.dto.CommentResponse;
import com.supportticket.dto.CreateCommentRequest;
import com.supportticket.dto.TicketDetail;
import com.supportticket.exception.TicketNotFoundException;
import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;

class CommentServiceTest extends ApiTestBase {

    @Autowired
    private CommentService commentService;

    @Autowired
    private TicketService ticketService;

    @ParameterizedTest
    @EnumSource(TicketStatus.class)
    void aCommentIsAcceptedOnTicketsInEveryStatus(TicketStatus status) {
        long id = saveTicket(status);

        CommentResponse comment = commentService.add(id, new CreateCommentRequest("Asha", "Looking into it"));

        assertThat(comment.id()).isNotNull();
        assertThat(comment.author()).isEqualTo("Asha");
        assertThat(comment.text()).isEqualTo("Looking into it");
        assertThat(comment.createdAt()).isNotNull();
        assertThat(ticketService.get(id).comments()).hasSize(1);
    }

    @Test
    void addingACommentDoesNotChangeTheTicketUpdatedAtOrVersion() {
        long id = saveTicket(TicketStatus.OPEN);
        TicketDetail before = ticketService.get(id);

        commentService.add(id, new CreateCommentRequest("Asha", "Note"));

        TicketDetail after = ticketService.get(id);
        assertThat(after.updatedAt()).isEqualTo(before.updatedAt());
        assertThat(after.version()).isEqualTo(before.version());
    }

    @Test
    void commentsAreShownOldestFirst() {
        long id = saveTicket(TicketStatus.OPEN);
        commentService.add(id, new CreateCommentRequest("A", "first"));
        commentService.add(id, new CreateCommentRequest("B", "second"));
        commentService.add(id, new CreateCommentRequest("A", "third"));

        assertThat(ticketService.get(id).comments())
                .extracting(CommentResponse::text)
                .containsExactly("first", "second", "third");
    }

    @Test
    void aCommentOnAMissingTicketIsRejected() {
        assertThatThrownBy(() -> commentService.add(999_999L, new CreateCommentRequest("A", "text")))
                .isInstanceOf(TicketNotFoundException.class);
        assertThat(commentRepository.count()).isZero();
    }
}
