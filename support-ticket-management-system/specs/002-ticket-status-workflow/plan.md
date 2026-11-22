# Implementation Plan: Ticket Status Workflow

**Branch**: `002-ticket-status-workflow` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/002-ticket-status-workflow/spec.md`

## Summary

Enforce the ticket status state machine (`OPEN → IN_PROGRESS → RESOLVED → CLOSED`, plus
`OPEN → CANCELLED` and `IN_PROGRESS → CANCELLED`) in the backend, reject every other transition
with a clear, structured error, and make the UI offer only valid next statuses.

Approach (details in [research.md](./research.md)):

- A single `TicketStatus` enum owns the transition table; `TicketService.changeStatus` is the only
  writer of `status`, so no client can bypass the rules.
- Transitions are persisted with one atomic conditional `UPDATE ... WHERE status = :from`, which is
  race-safe for simultaneous changes and the fastest option.
- Every ticket response carries `allowedNextStatuses`, so the Next.js UI renders only valid choices
  and never hard-codes the rules.
- Invalid transitions return `409 INVALID_STATUS_TRANSITION`; unknown values return
  `400 VALIDATION_ERROR`; both use one error shape the UI shows verbatim.
- Performance is handled by design: virtual threads, projection-based paged list queries, indexes,
  no N+1, compression, server-rendered responsive UI, and a small JavaScript budget.

Because no application code exists yet, this plan also fixes the project-wide stack and layout
(Java 21 + Spring Boot, H2, Next.js) that feature 001 will reuse. Feature 001 has no plan yet and
must be planned and implemented first or together with this feature (see Dependencies).

## Technical Context

**Language/Version**: Java 21 (backend); TypeScript on Node.js 20 LTS or later (frontend)

**Primary Dependencies**: Spring Boot 3.5.x (Web, Data JPA, Validation), Flyway, springdoc-openapi,
H2; Next.js (App Router) and React. No Lombok, no state-machine library, no UI or data-fetching
libraries. Versions are pinned at implementation.

**Storage**: H2 file mode at runtime (`./data/tickets`), H2 in-memory for tests; Flyway migrations
in portable SQL (`MODE=PostgreSQL`)

**Testing**: Backend: JUnit 5, Spring Boot Test, MockMvc, `@DataJpaTest`, `@SpringBootTest`.
Frontend: Vitest and React Testing Library (dev dependencies).

**Target Platform**: Linux/macOS/Windows server running a JVM; modern desktop and mobile browsers

**Project Type**: Web application (Spring Boot REST backend + Next.js frontend)

**Performance Goals**: Backend p95 under 100 ms (excluding network) for list, detail and status
change with 10,000 tickets; UI first contentful paint under 1.5 s on broadband; user-visible result
of a status change within 2 s for 95% of attempts (spec SC-005)

**Constraints**: Response time minimal by design (see research items 6 and 9); under 100 KB gzipped
JavaScript on the tickets list page; no secrets in source control; no permissive CORS; UI responsive
from 320 px width upward

**Scale/Scope**: About 10,000 tickets, tens of concurrent support users, 5 statuses, 5 allowed
transitions, 4 UI screens (list, detail, create, edit)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Checked against `.specify/memory/constitution.md` v1.0.0.

| Principle / rule                  | Status | Notes                                                                                           |
|-----------------------------------|--------|-------------------------------------------------------------------------------------------------|
| I. Simplicity First (MVP Scope)   | PASS   | Only the status workflow and what it needs; no history, reasons, reopen, or roles.               |
| II. Layered Spring Boot Backend   | PASS   | controller → service → repository; DTO records at the API boundary; enum holds the rules only.   |
| III. REST API Contract            | PASS   | `/api/v1`, Bean Validation, global exception handler, one error shape, UI uses API only.         |
| IV. Test What Matters             | PASS   | Parameterized test for all 20 status pairs plus concurrency and error cases, on H2.              |
| V. Secure by Default              | DEVIATION | Server-side validation, parameterized queries and no committed secrets are planned. Authentication and roles are not (separate feature). See Complexity Tracking. |
| Technology Stack: Database        | DEVIATION | H2 file mode at runtime (not in-memory only); PostgreSQL deferred. See Complexity Tracking.      |
| Technology Stack: Migrations      | PASS   | Flyway, no `hibernate.ddl-auto` schema generation.                                               |
| Technology Stack: Frontend        | PASS   | React with Next.js and TypeScript.                                                               |
| Dependency discipline             | PASS   | Each dependency is justified in research.md; Spring State Machine, Lombok, Tailwind rejected.     |
| Development Workflow              | ACTION | Repository is on `main`; create branch `002-ticket-status-workflow` before implementing.         |

**Gate result**: Passes with two documented deviations (below). No unresolved
`NEEDS CLARIFICATION` remains.

**Post-design re-check (after Phase 1)**: Unchanged. The data model, contract and quickstart add no
new dependencies or layers. The two deviations are still required and still justified.

## Project Structure

### Documentation (this feature)

```text
specs/002-ticket-status-workflow/
├── plan.md              # This file (/speckit-plan command output)
├── research.md          # Phase 0 output (/speckit-plan command)
├── data-model.md        # Phase 1 output (/speckit-plan command)
├── quickstart.md        # Phase 1 output (/speckit-plan command)
├── contracts/
│   └── openapi.yaml     # Phase 1 output (/speckit-plan command)
└── tasks.md             # Phase 2 output (/speckit-tasks command - NOT created by /speckit-plan)
```

### Source Code (repository root)

```text
backend/
├── pom.xml
├── mvnw, .mvn/
├── src/
│   ├── main/
│   │   ├── java/com/supportticket/
│   │   │   ├── SupportTicketApplication.java
│   │   │   ├── controller/        # TicketController (list, get, create, change status)
│   │   │   ├── service/           # TicketService (changeStatus is the only status writer)
│   │   │   ├── repository/        # TicketRepository (projection queries, conditional update)
│   │   │   ├── model/             # Ticket, Comment, TicketStatus, TicketPriority
│   │   │   ├── dto/               # records: TicketSummary, TicketDetail, ChangeStatusRequest, ApiError, ...
│   │   │   ├── exception/         # TicketNotFoundException, InvalidStatusTransitionException, GlobalExceptionHandler
│   │   │   └── config/            # OpenAPI and web configuration
│   │   └── resources/
│   │       ├── application.yml            # virtual threads, open-in-view=false, compression, H2 file URL
│   │       └── db/migration/V1__create_ticket_tables.sql
│   └── test/java/com/supportticket/
│       ├── model/TicketStatusTest.java            # all 20 ordered pairs, final statuses
│       ├── service/TicketServiceStatusTest.java   # unchanged status, not found, concurrent change
│       ├── repository/TicketRepositoryTest.java   # conditional update, filters, search escaping
│       └── controller/TicketControllerTest.java   # MockMvc: 200/400/404/409 and error shape
└── data/                          # H2 database files (git-ignored)

frontend/
├── package.json
├── next.config.ts                 # rewrites /api/v1/* to BACKEND_URL
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── tickets/
│   │   │   ├── page.tsx           # server-rendered list with ?q= &status= &page=
│   │   │   ├── loading.tsx
│   │   │   └── [id]/page.tsx      # detail with status control
│   │   └── error.tsx              # plain-language error fallback
│   ├── components/
│   │   ├── StatusSelector.tsx     # client component; renders allowedNextStatuses only
│   │   ├── StatusBadge.tsx
│   │   ├── TicketFilters.tsx      # debounced search + status filter (all five statuses)
│   │   └── ErrorMessage.tsx
│   └── lib/
│       ├── api.ts                 # fetch wrappers; turns ApiError into display messages
│       └── types.ts               # TicketStatus, TicketDetail, ApiError types
└── tests/
    ├── StatusSelector.test.tsx    # choices per status; none for final statuses
    └── ErrorMessage.test.tsx      # shows server rejection message
```

**Structure Decision**: Web application layout (separate `backend/` and `frontend/`), matching the
constitution's Spring Boot plus Next.js stack. The backend uses layer packages (`controller`,
`service`, `repository`, `model`, `dto`, `exception`, `config`) as required by Principle II. The
frontend uses the Next.js App Router with server components by default.

## Dependencies

- **Feature 001** (`specs/001-support-ticket-management`): defines create, update, comments, search
  and the base Ticket and Comment entities. It has no plan yet. Recommended order: run
  `/speckit-plan` for 001 (it will reuse the decisions here), implement 001 and 002 together or
  001 first. Feature 001's own spec must also be amended to use the five-status model.
- **Sign-in feature** (not yet specified): required before any non-local deployment.

## Complexity Tracking

> Filled because the Constitution Check has deviations that must be justified in writing.

| Deviation | Why Needed | Simpler / Compliant Alternative Rejected Because |
|-----------|------------|--------------------------------------------------|
| Principle V: no authentication or role checks on endpoints in this feature | The approved spec assigns sign-in to a separate feature; adding it here would double this feature's scope. Mitigation: build for local and development use only, do not deploy beyond that until the sign-in feature is merged; keep authorization out of the service layer so Spring Security can wrap the controllers later. | Adding temporary HTTP Basic auth is throwaway work and would hard-code credentials, which the secrets rules forbid. |
| Stack: H2 in file mode at runtime, PostgreSQL deferred | FR-011 requires data to survive a restart, which in-memory H2 cannot do; the user selected H2 as the database. Portable SQL and `MODE=PostgreSQL` keep a later move to PostgreSQL low-cost. Recommend amending the constitution (PATCH) to say "H2 file mode for local runtime" if this is accepted. | In-memory H2 at runtime fails FR-011; adding PostgreSQL now adds an external service the user did not request. |
