package com.supportticket.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class TicketStatusTest {

    /** The complete list of allowed transitions, written independently of the enum's own table. */
    private static final Set<String> ALLOWED = Set.of(
            "OPEN->IN_PROGRESS",
            "IN_PROGRESS->RESOLVED",
            "RESOLVED->CLOSED",
            "OPEN->CANCELLED",
            "IN_PROGRESS->CANCELLED");

    static Stream<Arguments> allOrderedPairsOfDifferentStatuses() {
        List<Arguments> pairs = new ArrayList<>();
        for (TicketStatus from : TicketStatus.values()) {
            for (TicketStatus to : TicketStatus.values()) {
                if (from != to) {
                    pairs.add(Arguments.of(from, to, ALLOWED.contains(from + "->" + to)));
                }
            }
        }
        return pairs.stream();
    }

    @Test
    void thereAreTwentyPairsFiveAcceptedAndFifteenRejected() {
        List<Arguments> pairs = allOrderedPairsOfDifferentStatuses().toList();
        long accepted = pairs.stream().filter(a -> (boolean) a.get()[2]).count();

        assertThat(pairs).hasSize(20);
        assertThat(accepted).isEqualTo(5);
    }

    @ParameterizedTest(name = "{0} -> {1} allowed={2}")
    @MethodSource("allOrderedPairsOfDifferentStatuses")
    void onlyTheFiveWorkflowTransitionsAreAllowed(TicketStatus from, TicketStatus to, boolean expected) {
        assertThat(from.canTransitionTo(to)).isEqualTo(expected);
    }

    @Test
    void sameStatusIsNotATransition() {
        for (TicketStatus status : TicketStatus.values()) {
            assertThat(status.canTransitionTo(status)).isFalse();
        }
    }

    @Test
    void closedAndCancelledAreFinalAndOthersAreNot() {
        assertThat(TicketStatus.CLOSED.isFinal()).isTrue();
        assertThat(TicketStatus.CANCELLED.isFinal()).isTrue();
        assertThat(TicketStatus.OPEN.isFinal()).isFalse();
        assertThat(TicketStatus.IN_PROGRESS.isFinal()).isFalse();
        assertThat(TicketStatus.RESOLVED.isFinal()).isFalse();
    }

    @Test
    void allowedNextListsMatchTheWorkflowInDisplayOrder() {
        assertThat(TicketStatus.OPEN.allowedNext())
                .containsExactly(TicketStatus.IN_PROGRESS, TicketStatus.CANCELLED);
        assertThat(TicketStatus.IN_PROGRESS.allowedNext())
                .containsExactly(TicketStatus.RESOLVED, TicketStatus.CANCELLED);
        assertThat(TicketStatus.RESOLVED.allowedNext()).containsExactly(TicketStatus.CLOSED);
        assertThat(TicketStatus.CLOSED.allowedNext()).isEmpty();
        assertThat(TicketStatus.CANCELLED.allowedNext()).isEmpty();
    }

    @Test
    void namedExamplesFromTheRequirementsAreRejected() {
        assertThat(TicketStatus.CLOSED.canTransitionTo(TicketStatus.OPEN)).isFalse();
        assertThat(TicketStatus.RESOLVED.canTransitionTo(TicketStatus.OPEN)).isFalse();
        assertThat(TicketStatus.CANCELLED.canTransitionTo(TicketStatus.OPEN)).isFalse();
    }
}
