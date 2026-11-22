# Research: Ticket Management with Status Workflow

**Feature**: `003-ticket-management-workflow` | **Date**: 2026-11-22

All `NEEDS CLARIFICATION` items from the Technical Context are resolved below. Stack choices come
from the user's planning input (Java 21, Spring Boot, H2, Next.js, responsive UI, minimal response
time) and the project constitution. This research carries forward and extends the decisions made for
`specs/002-ticket-status-workflow`.

## 1. Backend platform

- **Decision**: Java 21, Spring Boot 3.5.x (latest 3.5 patch, pinned in `pom.xml`), Maven with the
  Maven Wrapper (`./mvnw`), Spring Web, Spring Data JPA, Bean Validation, Flyway, springdoc-openapi.
- **Rationale**: Matches the constitution (Java 21, Spring Boot, JPA, Bean Validation, OpenAPI).
  3.5.x supports Java 21 and virtual threads. The wrapper pins Maven, which matters because the
  machine has Maven 3.6.3 and JDK 17 installed (see `quickstart.md`).
- **Alternatives considered**: Gradle (constitution says pick one; Maven chosen); Spring Boot 4.x
  (newer; 3.x is the family named in project guidance with the widest library support); WebFlux
  (more complexity and no benefit for a blocking JDBC/H2 workload).

## 2. Database

- **Decision**: H2 in **file mode** at runtime (`jdbc:h2:file:./data/tickets;MODE=PostgreSQL`), H2
  **in-memory** for automated tests, schema managed by Flyway in portable SQL. The H2 console is
  disabled. The DB password comes from the `TICKETS_DB_PASSWORD` environment variable, never from
  source control.
- **Rationale**: The user chose H2. FR-010 requires data to survive a restart, which in-memory H2
  cannot do. `MODE=PostgreSQL` and portable SQL keep a later move to PostgreSQL (the constitution's
  production target) cheap.
- **Alternatives considered**: In-memory H2 at runtime (fails FR-010); PostgreSQL now (not requested,
  adds an external service).
- **Constitution impact**: see Complexity Tracking in `plan.md`.

## 3. Where the state machine lives

- **Decision**: One `TicketStatus` enum (`OPEN`, `IN_PROGRESS`, `RESOLVED`, `CLOSED`, `CANCELLED`)
  owns the transition table as an immutable map, exposing `allowedNext()`, `canTransitionTo(target)`
  and `isFinal()`. `TicketService` is the only code that writes `status`. Both the dedicated status
  endpoint and the ticket update endpoint call the same transition guard. Create always sets `OPEN`
  and ignores any supplied status.
- **Rationale**: One source of truth satisfies FR-011 to FR-015 and cannot be bypassed by the UI
  (FR-014). An in-memory table lookup costs microseconds. `allowedNextStatuses` on every ticket
  response (FR-020) means the UI never hard-codes the rules.
- **Alternatives considered**: Spring State Machine (new dependency, heavy for 5 states and
  5 transitions); a DB trigger or CHECK on transitions (unreadable, poor error messages, not
  portable); rules in the frontend (explicitly not allowed).

## 4. Concurrency

Two different concurrent situations are handled with two simple mechanisms.

- **Status endpoint** (`PATCH /tickets/{id}/status`), FR-019. **Decision**: persist with one
  conditional update:
  `UPDATE ticket SET status = :to, updated_at = :now, version = version + 1 WHERE id = :id AND status = :from`.
  If 0 rows change, reload and re-evaluate once against the current status; the result is an
  accepted retry or `INVALID_STATUS_TRANSITION` naming the new current status.
  **Rationale**: Atomic and lock-free, so also the fastest; the second of two simultaneous changes
  is judged against the status at that moment.
- **Update endpoint** (`PUT /tickets/{id}`), FR-023. **Decision**: JPA `@Version` optimistic locking,
  with the client sending the `version` it loaded. A mismatch returns `409 CONCURRENT_MODIFICATION`.
  If the request also carries a different status, the transition is validated first against the
  loaded ticket; the version check then catches any concurrent status change.
  **Rationale**: The user needs to be told "someone else changed this ticket" rather than silently
  overwriting. A rejected request rolls back, so nothing changes (FR-016).
- **Alternatives considered**: Pessimistic `SELECT ... FOR UPDATE` (holds locks, slower under load);
  last-write-wins (violates FR-023).

## 5. HTTP semantics and error contract

- **Decision**:
  - Invalid transition between known statuses: `409`, `code = INVALID_STATUS_TRANSITION`.
  - Stale version on update: `409`, `code = CONCURRENT_MODIFICATION`.
  - Validation failure, unknown status or priority, or malformed body: `400`,
    `code = VALIDATION_ERROR`, with `fieldErrors` listing valid values (FR-022).
  - Missing ticket: `404`, `code = TICKET_NOT_FOUND`.
  - Unexpected failure: `500`, `code = INTERNAL_ERROR`, generic message, details only in the log.
  - Create returns `201` with a `Location` header; comment create returns `201`.
  - One error shape for everything via a `@RestControllerAdvice`:
    `{ timestamp, status, code, message, path, fieldErrors[], currentStatus?, requestedStatus?, allowedNextStatuses? }`.
  - Status and priority values travel as upper-case identifiers; the UI maps them to labels.
- **Rationale**: 409 means "conflicts with the resource's current state", which fits both conflict
  cases, while 400 stays for bad input (FR-022). One shape lets the UI show every failure the same
  way (FR-024) and satisfies the constitution's error-structure rule.
- **Alternatives considered**: 422 for transitions (defensible; 409 chosen for state-conflict
  semantics); 400 for everything (loses the distinction FR-022 requires).

## 6. Update semantics

- **Decision**: `PUT /api/v1/tickets/{id}` replaces the four editable fields (title, description,
  priority, assignee; assignee may be null to clear) and requires `version`. An optional `status`
  field is accepted and goes through the transition guard.
- **Rationale**: Matches an edit form, avoids the omitted-versus-null ambiguity of partial updates
  with Java records, and is the simplest contract that satisfies FR-005.
- **Alternatives considered**: `PATCH` with JSON merge semantics (needs extra handling to tell "not
  sent" from "set to null"); separate endpoint per field (chatty).

## 7. Search and filter

- **Decision**: `GET /api/v1/tickets?status=&q=&page=&size=` runs one query:
  `(:status IS NULL OR status = :status) AND (:q IS NULL OR LOWER(title) LIKE :p ESCAPE '!' OR LOWER(description) LIKE :p ESCAPE '\')`,
  where the service lower-cases the keyword and escapes `%`, `_` and the escape character `!` so every character is plain
  text (spec edge case). The query is parameterized; no string concatenation.
- **Rationale**: Meets FR-008 and FR-009 with no extra dependency. A leading-wildcard `LIKE` cannot
  use a B-tree index, which is acceptable at about 10,000 tickets and is measured in the quickstart.
- **Alternatives considered**: Full-text search (premature, not portable on H2); an external search
  engine (far beyond scope).

## 8. Backend performance ("minimal response time")

- **Decision**: Each measure is cheap and adds no dependency:
  - `spring.threads.virtual.enabled=true` (Java 21 virtual threads for request handling).
  - `spring.jpa.open-in-view=false`; `@Transactional(readOnly = true)` on reads.
  - The list returns a **projection** (no comments, no lazy loading), so no N+1 queries.
  - Comments are `LAZY` and loaded only for the detail view, with one ordered query.
  - Paging is mandatory: default `size=20`, max `size=100`, stable order `created_at DESC, id DESC`.
  - Indexes: `(status, created_at DESC)`, `(created_at DESC)`; comments `(ticket_id, created_at)`.
  - Status change is one conditional `UPDATE` (decision 4).
  - `server.compression.enabled=true` for JSON; HikariCP defaults; Jackson default configuration.
  - Input limits (title 150, description 5,000, comment 2,000, page size 100) bound response sizes.
- **Rationale**: Targets the stated goal with configuration and query design, not speculative
  caching.
- **Alternatives considered**: Spring Cache or Caffeine (stale-data risk, extra dependency, little
  gain at this size); keyset pagination (more UI complexity; revisit beyond about 100,000 rows).

## 9. Frontend

- **Decision**: Next.js (App Router) with TypeScript. React Server Components render the list and
  detail pages; small Client Components are used only for interaction (filters, forms, status
  selector, comment form). Styling uses CSS Modules with a mobile-first layout. No UI component
  library and no data-fetching library.
- **Rationale**: Server rendering sends ready HTML (fast first paint, little JavaScript) which serves
  "responsive" and "minimal response time". CSS Modules ship with Next.js, so no extra dependency or
  runtime styling cost.
- **Alternatives considered**: Tailwind (extra build dependency); MUI or Ant Design (large bundles);
  SWR or React Query (not needed for simple calls); SPA-only React (slower first load).

## 10. Frontend-to-backend wiring

- **Decision**: Browser calls go to same-origin `/api/v1/*`, which `next.config` rewrites to the
  Spring Boot server (URL from the `BACKEND_URL` environment variable). Server Components call
  `BACKEND_URL` directly.
- **Rationale**: Same-origin calls need no CORS configuration, so the backend never needs a
  permissive CORS policy. The UI talks to the backend only through the REST API, as the constitution
  requires.
- **Alternatives considered**: Enabling CORS on Spring Boot (more surface to secure); Next.js Route
  Handler proxy (extra code and an extra hop).

## 11. Frontend performance, responsiveness and error display

- **Decision**:
  - Mobile-first CSS from 320 px width; the list is stacked cards below 640 px and a table above.
  - Search input debounced (300 ms) and reflected in the URL (`?q=&status=&page=`) so results are
    server-rendered and linkable.
  - `loading.tsx` skeletons and `next/font` (self-hosted, no layout shift); no images or heavy
    libraries on ticket pages.
  - Status choices come only from the ticket's `allowedNextStatuses`; final tickets show none.
  - Forms show `fieldErrors` next to each field, keep entered text on failure, and show the server's
    `message` for conflicts and not-found. Unexpected failures show a retry option and never raw
    technical text.
  - Budget: first contentful paint under 1.5 s on broadband; under 200 KB gzipped JavaScript on the
    list page (measured: about 175 KB, of which nearly all is the React and Next.js runtime that every
    route ships; the first draft's 100 KB was not reachable with Next.js 16).
- **Rationale**: Keeps interaction instant and the bundle small without premature optimization, and
  covers User Story 7 directly.
- **Alternatives considered**: Client-side filtering of the whole list (does not scale, duplicates
  backend rules); infinite scroll (more JavaScript, harder to test).

## 12. Testing

- **Decision**:
  - Backend: JUnit 5, Spring Boot Test, MockMvc, `@DataJpaTest`, and one `@SpringBootTest`
    integration test, all on in-memory H2. A parameterized test covers **all 20 ordered status
    pairs** (5 accepted, 15 rejected), plus final statuses, unknown status, unchanged status,
    concurrent status changes, stale-version update, validation limits, search escaping and filters.
  - Frontend: Vitest with React Testing Library for the status selector, form error display and
    error message component (dev dependencies only).
  - Performance smoke check: documented in `quickstart.md`, run against 10,000 seeded tickets.
- **Rationale**: Proves SC-004 to SC-007 and satisfies the constitution's "Test What Matters".
- **Alternatives considered**: Playwright end-to-end tests (valuable later, heavy for this scope);
  Testcontainers (unnecessary while H2 is the database).

## 13. API documentation and code style

- **Decision**: The hand-written `contracts/openapi.yaml` is the design source. At implementation,
  springdoc-openapi (pinned) serves the generated spec, which is compared with the contract in
  review. DTOs are Java records; no Lombok.
- **Rationale**: Records give immutable DTOs with no extra dependency; springdoc is the
  constitution's documentation choice.
- **Alternatives considered**: Lombok (extra annotation processing, not needed with Java 21);
  generating controllers from the contract (more tooling than this scope needs).

## 14. Authentication

- **Decision**: Not part of this feature. Authorization stays out of the service layer so Spring
  Security can be placed in front of the controllers later.
- **Rationale**: The spec assigns sign-in to a separate feature. The deviation from Constitution
  Principle V is justified and time-boxed in `plan.md` Complexity Tracking.
- **Alternatives considered**: Temporary HTTP Basic auth (throwaway work and hard-coded credentials,
  which the secrets rules forbid); ignoring the principle silently (not allowed by Governance).

## 15. Relationship to earlier specs

- **Decision**: Treat `003` as the single source of truth. `specs/001-support-ticket-management` and
  `specs/002-ticket-status-workflow` (and the plan in 002) are superseded by this feature and should
  be marked as such or removed so they do not diverge.
- **Rationale**: Three overlapping specs would produce conflicting rules (001 allowed any status
  change).
- **Alternatives considered**: Keeping all three active (duplicate work, conflicting status rules).
