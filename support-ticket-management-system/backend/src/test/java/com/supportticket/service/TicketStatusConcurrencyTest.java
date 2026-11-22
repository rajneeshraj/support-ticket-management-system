package com.supportticket.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.supportticket.exception.InvalidStatusTransitionException;
import com.supportticket.model.TicketStatus;
import com.supportticket.support.ApiTestBase;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;

class TicketStatusConcurrencyTest extends ApiTestBase {

    @Autowired
    private TicketService ticketService;

    /**
     * RESOLVED and CANCELLED are mutually exclusive from IN_PROGRESS, so when both are requested at
     * the same moment exactly one must win and the other must be judged against the new status.
     */
    @RepeatedTest(15)
    void simultaneousConflictingChangesApplyOneAtATime() throws Exception {
        long id = saveTicket(TicketStatus.IN_PROGRESS);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<TicketStatus>> futures = new ArrayList<>();
            for (TicketStatus target : List.of(TicketStatus.RESOLVED, TicketStatus.CANCELLED)) {
                Callable<TicketStatus> task = () -> {
                    start.await();
                    return ticketService.changeStatus(id, target).status();
                };
                futures.add(pool.submit(task));
            }
            start.countDown();

            int accepted = 0;
            int rejected = 0;
            for (Future<TicketStatus> future : futures) {
                try {
                    future.get(10, TimeUnit.SECONDS);
                    accepted++;
                } catch (java.util.concurrent.ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(InvalidStatusTransitionException.class);
                    InvalidStatusTransitionException ex = (InvalidStatusTransitionException) e.getCause();
                    // The loser is judged against the status the winner produced, not the stale one.
                    assertThat(ex.getCurrentStatus()).isIn(TicketStatus.RESOLVED, TicketStatus.CANCELLED);
                    rejected++;
                }
            }

            assertThat(accepted).isEqualTo(1);
            assertThat(rejected).isEqualTo(1);
            assertThat(ticketService.get(id).status()).isIn(TicketStatus.RESOLVED, TicketStatus.CANCELLED);
        } finally {
            pool.shutdownNow();
        }
    }
}
