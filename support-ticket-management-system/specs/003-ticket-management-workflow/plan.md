# Implementation Plan: Ticket Management with Status Workflow

**Branch**: `003-ticket-management-workflow` | **Date**: 2026-11-22 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/003-ticket-management-workflow/spec.md`

## Summary

Build the complete Support Ticket Management System: create, list, view, update, comment on, search
and filter tickets, with data persisted in H2 and all rules validated on the server. The ticket
status follows a strict state machine (`OPEN → IN_PROGRESS → RESOLVED → CLOSED`, plus
`OPEN → CANCELLED` and `IN_PROGRESS → CANCELLED`) enforced by the backend, with every other
transition rejected by a clear error. The UI is a responsive Next.js app that shows meaningful
errors and offers only valid next statuses.

Approach (details in [research.md](./research.md)):

- **Backend**: Java 21 and Spring Boot 3.5.x as a layered REST API under `/api/v1`
  (controller → service → repository), DTO records at the boundary, Flyway migrations, and one global
  exception handler producing a single error shape.
- **State machine**: one `TicketStatus` enum owns the transition table; `TicketService` is the only
  writer of `status`, and both the status and update endpoints use the same guard.
- **Concurrency**: status changes use one atomic conditional `UPDATE ... WHERE status = :from`;
  edits use `@Version` optimistic locking so nobody silently overwrites a concurrent change.
- **Database**: H2 in file mode at runtime (survives restarts), in-memory for tests, portable SQL.
- **Frontend**: Next.js App Router with TypeScript, server-rendered pages, CSS Modules, mobile-first
  layout, same-origin `/api/v1` calls proxied to the backend (no CORS).
- **Performance** ("response time minimal") is designed in: virtual threads, projection-based paged
  list queries, indexes, no N+1, compression, bounded inputs, a small JavaScript budget, and a
  documented measurement in the quickstart.

This feature supersedes `specs/001-support-ticket-management` and `specs/002-ticket-status-workflow`
(including its plan); see Dependencies.

## Technical Context

**Language/Version**: Java 21 (backend); TypeScript on Node.js 20 LTS or later (frontend)

**Primary Dependencies**: Spring Boot 3.5.x (Web, Data JPA, Validation), Flyway, springdoc-openapi,
H2; Next.js (App Router) and React. No Lombok, state-machine library, UI library or data-fetching
library. Versions are pinned at implementation.

**Storage**: H2 file mode at runtime (`./data/tickets`), H2 in-memory for tests; Flyway migrations in
portable SQL (`MODE=PostgreSQL`)

**Testing**: Backend: JUnit 5, Spring Boot Test, MockMvc, `@DataJpaTest`, `@SpringBootTest`.
Frontend: Vitest and React Testing Library (dev dependencies).

**Target Platform**: Server running a JVM (Linux/macOS/Windows); modern desktop and mobile browsers

**Project Type**: Web application (Spring Boot REST backend + Next.js frontend)

**Performance Goals**: Backend p95 under 100 ms (excluding network) for list, search, detail, update,
comment and status change with 10,000 tickets; UI first contentful paint under 1.5 s on broadband;
user-visible results within 2 s for 95% of requests (spec SC-003)

**Constraints**: Response time minimal by design (research items 8 and 11); under 200 KB gzipped
JavaScript on the list page (framework baseline, see research item 11); responsive from 320 px width; no secrets in source control; no
permissive CORS; all validation enforced server-side

**Scale/Scope**: About 10,000 tickets, tens of concurrent support users, 5 statuses, 5 allowed
transitions, 5 API resources, 4 UI screens (list, create, detail with comments, edit)

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

Checked against `.specify/memory/constitution.md` v1.0.0.

| Principle / rule                  | Status    | Notes                                                                                           |
|-----------------------------------|-----------|-------------------------------------------------------------------------------------------------|
| I. Simplicity First (MVP Scope)   | PASS      | Only the specified capabilities; no history, reasons, reopen, attachments or roles.              |
| II. Layered Spring Boot Backend   | PASS      | controller → service → repository; DTO records at the API boundary; entities never exposed.      |
| III. REST API Contract            | PASS      | `/api/v1`, Bean Validation, global exception handler, one error shape, UI uses the API only.     |
| IV. Test What Matters             | PASS      | All 20 status pairs, concurrency, validation, search and endpoint tests, on H2.                  |
| V. Secure by Default              | DEVIATION | Server-side validation, parameterized queries and no committed secrets are planned. Authentication and roles are not (separate feature). See Complexity Tracking. |
| Technology Stack: Database        | DEVIATION | H2 file mode at runtime (not in-memory only); PostgreSQL deferred. See Complexity Tracking.      |
| Technology Stack: Migrations      | PASS      | Flyway, no `hibernate.ddl-auto` schema generation.                                               |
| Technology Stack: API docs        | PASS      | OpenAPI contract plus springdoc.                                                                 |
| Technology Stack: Frontend        | PASS      | React with Next.js and TypeScript.                                                               |
| Dependency discipline             | PASS      | Each dependency justified in research.md; Spring State Machine, Lombok, Tailwind rejected.        |
| Development Workflow              | ACTION    | Repository is on `main`; create branch `003-ticket-management-workflow` before implementing.     |

**Gate result**: Passes with two documented deviations (below). No unresolved
`NEEDS CLARIFICATION` remains.

**Post-design re-check (after Phase 1)**: Unchanged. The data model, contract and quickstart add no
new dependencies or layers beyond those justified in research.md. Both deviations are still required
and still justified.

## Project Structure

### Documentation (this feature)

```text
specs/003-ticket-management-workflow/
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
│   │   │   ├── controller/   # TicketController, CommentController
│   │   │   ├── service/      # TicketService (only writer of status), CommentService
│   │   │   ├── repository/   # TicketRepository (projection + search query, conditional update), CommentRepository
│   │   │   ├── model/        # Ticket, Comment, TicketStatus (transition table), TicketPriority
│   │   │   ├── dto/          # records: CreateTicketRequest, UpdateTicketRequest, ChangeStatusRequest,
│   │   │   │                 #          CreateCommentRequest, TicketSummary, TicketDetail, TicketPage, ApiError
│   │   │   ├── exception/    # TicketNotFoundException, InvalidStatusTransitionException, GlobalExceptionHandler
│   │   │   └── config/       # OpenAPI configuration
│   │   └── resources/
│   │       ├── application.yml                  # virtual threads, open-in-view=false, compression, H2 file URL
│   │       └── db/migration/V1__create_ticket_tables.sql
│   └── test/java/com/supportticket/
│       ├── model/TicketStatusTest.java          # all 20 ordered pairs, final statuses
│       ├── service/TicketServiceTest.java       # create, update, unchanged status, not found, concurrency
│       ├── repository/TicketRepositoryTest.java # conditional update, filter, search escaping, ordering
│       └── controller/TicketControllerTest.java # MockMvc: 200/201/400/404/409 and the error shape
└── data/                                        # H2 files (git-ignored)

frontend/
├── package.json
├── next.config.ts                # rewrites /api/v1/* to BACKEND_URL
├── src/
│   ├── app/
│   │   ├── layout.tsx
│   │   ├── error.tsx             # plain-language fallback with retry
│   │   └── tickets/
│   │       ├── page.tsx          # server-rendered list with ?q= &status= &page=
│   │       ├── loading.tsx
│   │       ├── new/page.tsx      # create form
│   │       └── [id]/
│   │           ├── page.tsx      # detail, status control, comments
│   │           └── edit/page.tsx # edit form
│   ├── components/
│   │   ├── TicketFilters.tsx     # debounced search + status filter (all five statuses)
│   │   ├── TicketList.tsx        # table on wide screens, cards below 640 px
│   │   ├── TicketForm.tsx        # create/edit, shows field errors, keeps entered text
│   │   ├── StatusSelector.tsx    # renders allowedNextStatuses only; none for final tickets
│   │   ├── StatusBadge.tsx
│   │   ├── CommentList.tsx
│   │   ├── CommentForm.tsx
│   │   ├── Pagination.tsx
│   │   └── ErrorMessage.tsx
│   └── lib/
│       ├── api.ts                # fetch wrappers; converts ApiError into display messages
│       └── types.ts              # TicketStatus, TicketDetail, ApiError types
└── tests/
    ├── StatusSelector.test.tsx   # choices per status; none for final statuses
    ├── TicketForm.test.tsx       # shows field errors, keeps entered text
    └── ErrorMessage.test.tsx     # shows server rejection message
```

**Structure Decision**: Web application layout (separate `backend/` and `frontend/`) matching the
constitution's Spring Boot plus Next.js stack. The backend uses layer packages (`controller`,
`service`, `repository`, `model`, `dto`, `exception`, `config`) as required by Principle II. The
frontend uses the Next.js App Router with server components by default and client components only
for interaction.

## Dependencies

- **Supersedes** `specs/001-support-ticket-management` and `specs/002-ticket-status-workflow`
  (spec, plan and design artifacts). Mark them superseded or remove them so rules do not diverge;
  001 in particular allowed any status change, which this feature forbids.
- **Sign-in feature** (not yet specified): required before any non-local deployment.
- **Tooling**: JDK 21 must be installed (the machine has JDK 17); Node.js 20 or later (24 is
  installed); Maven comes from the wrapper.

## Complexity Tracking

> Filled because the Constitution Check has deviations that must be justified in writing.

| Deviation | Why Needed | Simpler / Compliant Alternative Rejected Because |
|-----------|------------|--------------------------------------------------|
| Principle V: no authentication or role checks on endpoints in this feature | The approved spec assigns sign-in to a separate feature; adding it here would greatly widen scope. Mitigation: run for local and development use only and do not deploy beyond that until sign-in is merged; keep authorization out of the service layer so Spring Security can wrap the controllers later. | Temporary HTTP Basic auth is throwaway work and would hard-code credentials, which the secrets rules forbid. |
| Stack: H2 in file mode at runtime, PostgreSQL deferred | FR-010 requires data to survive a restart, which in-memory H2 cannot do; the user selected H2. Portable SQL and `MODE=PostgreSQL` keep a later move to PostgreSQL low-cost. Recommend a PATCH amendment of the constitution to say "H2 file mode for local runtime" if accepted. | In-memory H2 at runtime fails FR-010; PostgreSQL now adds an external service the user did not request. |
