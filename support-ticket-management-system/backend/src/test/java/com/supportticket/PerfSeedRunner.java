package com.supportticket;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Seeds 10,000 mixed-status tickets for the performance smoke check. Test sources only, active
 * only with the {@code perf} profile (in-memory database, see application-perf.yml):
 *
 * <pre>./mvnw spring-boot:test-run -Dspring-boot.run.profiles=perf</pre>
 */
@Component
@Profile("perf")
class PerfSeedRunner implements ApplicationRunner {

    private static final int TICKETS = 10_000;
    private static final String[] STATUSES = {"OPEN", "IN_PROGRESS", "RESOLVED", "CLOSED", "CANCELLED"};
    private static final String[] PRIORITIES = {"LOW", "MEDIUM", "HIGH", "CRITICAL"};
    private static final String[] TOPICS = {"printer", "login", "invoice", "network", "password", "laptop"};

    private final JdbcTemplate jdbc;

    PerfSeedRunner(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        Instant base = Instant.now().minus(TICKETS, ChronoUnit.MINUTES);
        List<Object[]> rows = new ArrayList<>(TICKETS);
        for (int i = 0; i < TICKETS; i++) {
            String topic = TOPICS[i % TOPICS.length];
            Timestamp created = Timestamp.from(base.plus(i, ChronoUnit.MINUTES));
            rows.add(new Object[] {
                "Issue " + i + " with " + topic,
                "Customer reports a problem with the " + topic + " (case " + i + "). Please investigate.",
                STATUSES[i % STATUSES.length],
                PRIORITIES[i % PRIORITIES.length],
                i % 3 == 0 ? "Agent " + (i % 10) : null,
                created,
                created
            });
        }
        jdbc.batchUpdate(
                "insert into ticket (title, description, status, priority, assignee, created_at, updated_at) "
                        + "values (?, ?, ?, ?, ?, ?, ?)",
                rows);
        System.out.println("PerfSeedRunner: seeded " + TICKETS + " tickets.");
    }
}
