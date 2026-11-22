# Research: Ticket Status Workflow

**Feature**: `002-ticket-status-workflow` | **Date**: 2026-11-22

All `NEEDS CLARIFICATION` items from the Technical Context are resolved below. Stack choices come
from the user's planning input (Java 21, Spring Boot, H2, Next.js, responsive UI, minimal response
time) and the project constitution.

## 1. Backend platform

- **Decision**: Java 21, Spring Boot 3.5.x (latest 3.5 patch, pinned in `pom.xml`), Maven with the
  Maven Wrapper (`./mvnw`), Spring Web, Spring Data JPA, Bean Validation, springdoc-openapi.
- **Rationale**: Matches the constitution (Java 21, Spring Boot, JPA, Bean Validation, OpenAPI).
  3.5.x supports Java 21 and virtual threads. The wrapper pins the Maven version, which matters
  because the machine has Maven 3.6.3 and JDK 17 installed (see `quickstart.md`).
- **Alternatives considered**: Gradle (no advantage here, constitution says pick one: Maven);
  Spring Boot 4.x (newer, but 3.x is the version family named in project guidance and has the widest
  library support); WebFlux (more complexity, no benefit for a blocking JDBC/H2 workload).

## 2. Database

- **Decision**: H2 in **file mode** for the runtime profile (`jdbc:h2:file:./data/tickets;MODE=PostgreSQL`),
  H2 **in-memory** for automated tests. Schema is managed by Flyway migrations written in portable
  SQL. The H2 console is disabled. The DB password comes from the `TICKETS_DB_PASSWORD` environment
  variable, never from source control.
- **Rationale**: The user chose H2. FR-011 requires data to survive a restart, which in-memory H2
  cannot do, so file mode is needed at runtime. `MODE=PostgreSQL` and portable SQL keep a later move
  to PostgreSQL (constitution's production target) cheap.
- **Alternatives considered**: In-memory H2 at runtime (fails FR-011); PostgreSQL now (user asked
  for H2; adds an external service for no current benefit).
- **Constitution impact**: see Complexity Tracking in `plan.md` (file-mode H2 at runtime;
  PostgreSQL deferred).

## 3. Where the state machine lives

- **Decision**: A single `TicketStatus` enum (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`,
  `CANCELLED`) owns the transition table as an immutable map of allowed targets, exposing
  `allowedNext()`, `canTransitionTo(target)` and `isFinal()`. `TicketService.changeStatus` is the
  only code path that writes `status`. Any endpoint that accepts a status (status endpoint, ticket
  update) calls it. Create always sets `OPEN` and ignores any supplied status.
- **Rationale**: One source of truth satisfies FR-003 to FR-006 and cannot be bypassed by the UI
  (FR-005). A pure in-memory table check costs microseconds, so enforcement adds no latency.
  `allowedNextStatuses` on every ticket response (FR-013) lets the UI avoid hard-coding the rules.
- **Alternatives considered**: Spring State Machine (new dependency, heavy for 5 states and
  5 transitions, violates dependency discipline); DB trigger or CHECK on transitions (hard to read,
  hard to produce meaningful errors, not portable); rules in the frontend (explicitly not allowed).

## 4. Concurrency of simultaneous status changes (FR-012)

- **Decision**: Persist a transition with one conditional update:
  `UPDATE ticket SET status = :to, updated_at = :now, version = version + 1 WHERE id = :id AND status = :from`.
  If 0 rows change, reload the ticket and re-evaluate against its current status once; the result is
  an accepted retry or an `INVALID_STATUS_TRANSITION` response naming the new current status.
- **Rationale**: Atomic and race-safe without holding locks, so it is also the fastest option. The
  second of two simultaneous changes is judged against the status at that moment, exactly as FR-012
  requires.
- **Alternatives considered**: Pessimistic `SELECT ... FOR UPDATE` (holds a lock for the whole
  transaction, slower under load); JPA `@Version` alone (would reject the loser with a generic
  conflict instead of a status-aware answer).

## 5. HTTP semantics and error contract

- **Decision**:
  - Invalid but known transition: `409 Conflict`, `code = INVALID_STATUS_TRANSITION`.
  - Unknown status value or malformed body: `400 Bad Request`, `code = VALIDATION_ERROR`, with
    `fieldErrors` listing valid values (FR-009).
  - Missing ticket: `404 Not Found`, `code = TICKET_NOT_FOUND`.
  - One error shape for everything, produced by a `@RestControllerAdvice`:
    `{ timestamp, status, code, message, path, fieldErrors[], currentStatus?, requestedStatus?, allowedNextStatuses? }`.
  - Status values travel as the upper-case identifiers `OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`,
    `CANCELLED`. The UI maps them to display labels.
- **Rationale**: 409 states "the request conflicts with the ticket's current state", which is what
  an invalid transition is, and keeps it distinct from 400 input errors. One shape lets the UI show
  every failure the same way (FR-014/FR-015) and satisfies the constitution's error-structure rule.
- **Alternatives considered**: 422 (also defensible, 409 chosen for state-conflict semantics);
  400 for everything (loses the distinction FR-009 requires).

## 6. Backend performance ("minimal response time")

- **Decision**: Apply these measures; each is cheap and none adds a dependency:
  - `spring.threads.virtual.enabled=true` (Java 21 virtual threads for request handling).
  - `spring.jpa.open-in-view=false` and `@Transactional(readOnly = true)` on reads.
  - List endpoint returns a **projection DTO** (no comments, no lazy loading), so no N+1 queries.
  - Comments are `LAZY` and loaded only by the detail endpoint with one query.
  - Paging is mandatory: default `size=20`, max `size=100`, stable order `created_at DESC, id DESC`.
  - Indexes: `(status, created_at DESC)`, `(created_at DESC)`; comments `(ticket_id, created_at)`.
  - Search uses `LOWER(col) LIKE :pattern ESCAPE '\'` with `%`, `_` and `\` escaped by the service,
    so special characters are plain text (spec edge case) and the query stays parameterized.
  - Status change is one `UPDATE` (see decision 4), no read-modify-write round trip on the happy
    path beyond the single lookup needed for the check.
  - `server.compression.enabled=true` for JSON responses; HikariCP defaults (pool of 10).
- **Rationale**: Targets the stated goal (minimal latency) with configuration and query design, not
  speculative caching. A leading-wildcard `LIKE` cannot use a B-tree index, which is acceptable at
  the specified scale (about 10,000 tickets) and is measured in the quickstart.
- **Alternatives considered**: Spring Cache or Caffeine (stale-data risk for little gain at this
  size, new dependency); full-text search (premature, not portable on H2); keyset pagination (adds
  UI complexity; revisit if lists exceed about 100,000 rows).

## 7. Frontend

- **Decision**: Next.js (App Router) with TypeScript, React Server Components for the ticket list
  and detail pages, and small Client Components only where interaction is needed (status selector,
  search box, error banner). Styling with CSS Modules and a mobile-first layout using media
  queries. No UI component library, no data-fetching library.
- **Rationale**: Server rendering sends ready HTML (fast first paint, little JavaScript), which
  directly serves the "responsive and minimal response time" goal. CSS Modules ship with Next.js,
  so there is zero extra dependency and no runtime styling cost.
- **Alternatives considered**: Tailwind (extra build dependency); MUI or Ant Design (large bundles);
  SWR or React Query (not needed for a handful of simple calls); SPA-only React (slower first load).

## 8. Frontend-to-backend wiring

- **Decision**: Browser calls go to same-origin `/api/v1/*`, which `next.config` rewrites to the
  Spring Boot server (URL from the `BACKEND_URL` environment variable). Server Components call
  `BACKEND_URL` directly.
- **Rationale**: Same-origin calls need no CORS configuration, so the backend never needs a
  permissive CORS policy (security rule). The UI talks to the backend only through the REST API, as
  the constitution requires.
- **Alternatives considered**: Enabling CORS on Spring Boot (extra surface to secure); a Next.js
  Route Handler proxy layer (extra code and an extra hop).

## 9. Frontend performance and responsiveness

- **Decision**:
  - Mobile-first CSS; the list collapses to stacked cards under 640 px and is a table above.
  - Search input is debounced (300 ms) and updates the URL query (`?q=&status=&page=`), so results
    are server-rendered and linkable.
  - `loading.tsx` skeletons and `next/font` (self-hosted, no layout shift); no images or heavy
    libraries on the ticket pages.
  - Status choices come from the ticket's `allowedNextStatuses`; final tickets render no control.
  - Performance budget: first contentful paint under 1.5 s on a broadband connection and under
    100 KB of gzipped JavaScript on the tickets list page.
- **Rationale**: Keeps interaction instantaneous and the bundle small without premature
  optimization.
- **Alternatives considered**: Client-side filtering of the whole list (does not scale, duplicates
  backend rules); infinite scroll (more JavaScript, harder to test).

## 10. Testing

- **Decision**:
  - Backend: JUnit 5, Spring Boot Test, MockMvc for the web layer, `@DataJpaTest` for repository
    and the conditional update, plus a `@SpringBootTest` integration test, all on in-memory H2.
    A parameterized test covers **all 20 ordered status pairs** (5 accepted, 15 rejected), plus
    final-status, unknown-status, concurrent-change and unchanged-status cases.
  - Frontend: Vitest with React Testing Library for the status selector and error display.
    Dev dependencies only.
  - Performance smoke check: a documented script in `quickstart.md` that times the list, detail and
    status endpoints against 10,000 seeded tickets.
- **Rationale**: Directly proves SC-001 and SC-002 and the constitution's "Test What Matters".
- **Alternatives considered**: Playwright end-to-end tests (valuable later, heavy for this
  feature); Testcontainers (not needed while H2 is the database).

## 11. API documentation and code style

- **Decision**: Hand-authored contract in `contracts/openapi.yaml` is the design source; at
  implementation springdoc-openapi (pinned) serves the generated spec, which is diffed against the
  contract in review. DTOs are Java records; no Lombok.
- **Rationale**: Records give immutable DTOs with no extra dependency; springdoc is the
  constitution's documentation choice.
- **Alternatives considered**: Lombok (extra annotation processing, not needed with records and
  Java 21); generating controllers from the contract (more tooling than this scope needs).

## 12. Authentication

- **Decision**: Not part of this feature. Endpoints are built so Spring Security can be added in
  front without changes to the service layer.
- **Rationale**: The spec assigns sign-in to a separate feature. The resulting deviation from
  Constitution Principle V is justified and time-boxed in `plan.md` Complexity Tracking.
- **Alternatives considered**: Adding minimal HTTP Basic auth now (extra scope and throwaway work);
  silently ignoring the principle (not allowed by the Governance section).
