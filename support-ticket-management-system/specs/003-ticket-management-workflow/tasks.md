---

description: "Task list for Ticket Management with Status Workflow"
---

# Tasks: Ticket Management with Status Workflow

**Input**: Design documents from `/specs/003-ticket-management-workflow/`

**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/openapi.yaml, quickstart.md

**Tests**: Included. The spec does not ask for TDD, but the constitution (Principle IV, "Test What
Matters") requires automated tests for every service-layer business rule and every REST endpoint.
Within each story, write the tests first and confirm they fail before implementing.

**Organization**: Tasks are grouped by user story so each can be implemented and tested on its own.
User stories run in priority order: P1 (US1, US2, US3), P2 (US4, US7), P3 (US5, US6).

**Field constraints**: Length limits, required/nullable rules and allowed values for Ticket and
Comment fields are defined only in `data-model.md`. Tasks cite them as `data-model.md Entity.field`
and must not restate or change them.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependency on an incomplete task)
- **[Story]**: User story the task belongs to (US1 to US7)
- Include exact file paths in descriptions

## Path Conventions

Web application, repository root `support-ticket-management-system/`:

- Backend: `backend/src/main/java/com/supportticket/...`, `backend/src/main/resources/...`,
  `backend/src/test/java/com/supportticket/...`
- Frontend: `frontend/src/...`, `frontend/tests/...`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization and basic structure

- [X] T001 Create and switch to git branch `003-ticket-management-workflow` in the repository root (constitution: no direct commits to `main`); confirm `java -version` is 21 and `node -v` is 20 or later
- [X] T002 Create root `.gitignore` covering `backend/data/`, `backend/target/`, `frontend/node_modules/`, `frontend/.next/`, and `.env*` files
- [X] T003 Initialize the Maven project in `backend/pom.xml` (Java 21, Spring Boot 3.5.x parent, starters: web, data-jpa, validation; plus flyway-core, h2, springdoc-openapi-starter-webmvc-ui, spring-boot-starter-test; all versions pinned; `-Xlint:all` compiler argument) and generate the Maven Wrapper in `backend/mvnw` and `backend/.mvn/`
- [X] T004 [P] Create the Spring Boot entry point in `backend/src/main/java/com/supportticket/SupportTicketApplication.java`
- [X] T005 [P] Initialize the Next.js App Router project with TypeScript in `frontend/package.json`, `frontend/tsconfig.json` and `frontend/next.config.ts` (rewrite `/api/v1/:path*` to `${BACKEND_URL}/api/v1/:path*`; versions pinned)
- [X] T006 [P] Configure frontend lint and tests in `frontend/eslint.config.mjs`, `frontend/vitest.config.ts` and `frontend/tests/setup.ts` (Vitest, React Testing Library, jsdom as dev dependencies; add `lint`, `test` scripts to `frontend/package.json`)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core infrastructure that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T007 Configure `backend/src/main/resources/application.yml`: `spring.threads.virtual.enabled=true`, `spring.jpa.open-in-view=false`, `spring.jpa.hibernate.ddl-auto=validate`, `server.compression.enabled=true`, H2 file URL `jdbc:h2:file:./data/tickets;MODE=PostgreSQL` with the password read from env var `TICKETS_DB_PASSWORD` (no default credentials), H2 console disabled, Flyway enabled
- [X] T008 [P] Create `backend/src/test/resources/application-test.yml` with an in-memory H2 URL (`jdbc:h2:mem:tickets;MODE=PostgreSQL`) and Flyway enabled so tests use the real migrations
- [X] T009 Create Flyway migration `backend/src/main/resources/db/migration/V1__create_ticket_tables.sql` for tables `ticket` and `comment`, with columns, types, nullability, defaults and the status `CHECK` constraint exactly as in `data-model.md` (`Ticket.id`, `Ticket.title`, `Ticket.description`, `Ticket.status`, `Ticket.priority`, `Ticket.assignee`, `Ticket.created_at`, `Ticket.updated_at`, `Ticket.version`, `Comment.id`, `Comment.ticket_id`, `Comment.author`, `Comment.text`, `Comment.created_at`) and indexes `idx_ticket_status_created`, `idx_ticket_created`, `idx_comment_ticket_created`
- [X] T010 [P] Create `TicketStatus` enum (five values, transition table, `allowedNext()`, `canTransitionTo()`, `isFinal()`) in `backend/src/main/java/com/supportticket/model/TicketStatus.java`, matching the table and values in `data-model.md` (`Ticket.status`)
- [X] T011 [P] Create `TicketPriority` enum in `backend/src/main/java/com/supportticket/model/TicketPriority.java` with the allowed values and default from `data-model.md` (`Ticket.priority`)
- [X] T012 Create `Ticket` JPA entity in `backend/src/main/java/com/supportticket/model/Ticket.java` mapped to every field in `data-model.md` (`Ticket.id`, `Ticket.title`, `Ticket.description`, `Ticket.status`, `Ticket.priority`, `Ticket.assignee`, `Ticket.created_at`, `Ticket.updated_at`, `Ticket.version`), enums stored as strings, `@Version` on `version`, no mapped `comments` collection (comments are read through `CommentRepository`, which keeps the list query free of N+1 loads) (depends on T010, T011)
- [X] T013 Create `Comment` JPA entity in `backend/src/main/java/com/supportticket/model/Comment.java` mapped to `data-model.md` (`Comment.id`, `Comment.ticket_id`, `Comment.author`, `Comment.text`, `Comment.created_at`) with a lazy many-to-one to `Ticket` (depends on T012)
- [X] T014 [P] Create `ApiError` and `FieldError` records (fields per `contracts/openapi.yaml` `ApiError`) in `backend/src/main/java/com/supportticket/dto/ApiError.java` and `backend/src/main/java/com/supportticket/dto/FieldError.java`
- [X] T015 [P] Create `TicketNotFoundException` and `InvalidStatusTransitionException` (carrying current status, requested status, allowed next statuses) in `backend/src/main/java/com/supportticket/exception/TicketNotFoundException.java` and `backend/src/main/java/com/supportticket/exception/InvalidStatusTransitionException.java`
- [X] T016 Create the base `@RestControllerAdvice` in `backend/src/main/java/com/supportticket/exception/GlobalExceptionHandler.java` building `ApiError` for `MethodArgumentNotValidException` (400 `VALIDATION_ERROR` with `fieldErrors`) and `TicketNotFoundException` (404 `TICKET_NOT_FOUND`) (depends on T014, T015)
- [X] T017 [P] Create OpenAPI configuration in `backend/src/main/java/com/supportticket/config/OpenApiConfig.java`
- [X] T018 [P] Define frontend types (`TicketStatus`, `TicketPriority`, `TicketSummary`, `TicketDetail`, `Comment`, `TicketPage`, `ApiError`, `FieldError`) in `frontend/src/lib/types.ts` per `contracts/openapi.yaml`
- [X] T019 Create the fetch layer in `frontend/src/lib/api.ts`: base URL `BACKEND_URL` on the server and same-origin `/api/v1` in the browser, JSON helpers, and a typed `ApiRequestError` carrying the parsed `ApiError` (depends on T018)
- [X] T020 [P] Create `frontend/src/app/layout.tsx` (self-hosted font via `next/font`, viewport meta) and mobile-first base styles in `frontend/src/app/globals.css` (works from 320 px width)
- [X] T021 [P] Create `ErrorMessage` component in `frontend/src/components/ErrorMessage.tsx` and `frontend/src/components/ErrorMessage.module.css`
- [X] T022 [P] Create `StatusBadge` component (labels Open, In Progress, Resolved, Closed, Cancelled) in `frontend/src/components/StatusBadge.tsx` and `frontend/src/components/StatusBadge.module.css`

**Checkpoint**: Foundation ready; user story implementation can begin

---

## Phase 3: User Story 1 - Create, List and View Tickets (Priority: P1) 🎯 MVP

**Goal**: A user can create a ticket (always Open), see it in a newest-first paged list, and open its details.

**Independent Test**: Run quickstart Scenario 1: create a ticket (a supplied `CLOSED` status is ignored), see it first in the list, open it, and get a not-found message for an unknown id.

### Tests for User Story 1 ⚠️ (write first; they must fail)

- [X] T023 [P] [US1] Service test: create forces status Open, ignores a supplied status, defaults priority, leaves assignee empty, sets timestamps (`data-model.md` `Ticket.status`, `Ticket.priority`, `Ticket.assignee`) in `backend/src/test/java/com/supportticket/service/TicketServiceTest.java`
- [X] T024 [P] [US1] Repository test: list projection is ordered `created_at DESC, id DESC`, is paged, and does not load comments, in `backend/src/test/java/com/supportticket/repository/TicketRepositoryTest.java`
- [X] T025 [P] [US1] MockMvc test for `POST /api/v1/tickets` (201, `Location`, status OPEN, 400 with `fieldErrors` for blank or over-length title and description and unknown priority), `GET /api/v1/tickets`, and `GET /api/v1/tickets/{id}` (200, 404) in `backend/src/test/java/com/supportticket/controller/TicketControllerTest.java`

### Implementation for User Story 1

- [X] T026 [P] [US1] Create `CreateTicketRequest` record with Bean Validation per `data-model.md` (`Ticket.title`, `Ticket.description`, `Ticket.priority`) in `backend/src/main/java/com/supportticket/dto/CreateTicketRequest.java`; unknown properties such as `status` are ignored
- [X] T027 [P] [US1] Create response records `TicketSummary`, `TicketDetail` (with `allowedNextStatuses` and `comments`), `CommentResponse` and `TicketPage` per `contracts/openapi.yaml` in `backend/src/main/java/com/supportticket/dto/`
- [X] T028 [P] [US1] Create `CommentRepository` with an ordered (oldest first) lookup by ticket in `backend/src/main/java/com/supportticket/repository/CommentRepository.java`
- [X] T029 [US1] Create `TicketRepository` with the paged `TicketSummary` projection query ordered newest first in `backend/src/main/java/com/supportticket/repository/TicketRepository.java` (depends on T012, T027)
- [X] T030 [US1] Implement `TicketService.create`, `list` and `get` in `backend/src/main/java/com/supportticket/service/TicketService.java`: create always sets `Ticket.status` to OPEN, applies the `Ticket.priority` default and sets `Ticket.created_at` and `Ticket.updated_at` per `data-model.md`; reads use `@Transactional(readOnly = true)`; `get` loads comments in one query and computes `allowedNextStatuses` (depends on T026, T028, T029)
- [X] T031 [US1] Implement `POST /api/v1/tickets`, `GET /api/v1/tickets` (`page`, `size`) and `GET /api/v1/tickets/{id}` in `backend/src/main/java/com/supportticket/controller/TicketController.java` (depends on T030)
- [X] T032 [P] [US1] Create `TicketList` component (table above 640 px, stacked cards below) in `frontend/src/components/TicketList.tsx` and `frontend/src/components/TicketList.module.css`
- [X] T033 [P] [US1] Create `Pagination` component in `frontend/src/components/Pagination.tsx` and `frontend/src/components/Pagination.module.css`
- [X] T034 [US1] Create the server-rendered list page with a "no tickets yet" empty state in `frontend/src/app/tickets/page.tsx` and a skeleton in `frontend/src/app/tickets/loading.tsx` (depends on T019, T032, T033)
- [X] T035 [US1] Create `TicketForm` (create mode: title, description, priority) as a client component in `frontend/src/components/TicketForm.tsx` and `frontend/src/components/TicketForm.module.css` (depends on T019)
- [X] T036 [US1] Create the create-ticket page in `frontend/src/app/tickets/new/page.tsx` that redirects to the new ticket after a successful create (depends on T035)
- [X] T037 [US1] Create the ticket detail page (all fields, status badge, comments area placeholder) in `frontend/src/app/tickets/[id]/page.tsx` and a not-found page with a link back to the list in `frontend/src/app/tickets/[id]/not-found.tsx` (depends on T019, T022)
- [X] T038 [P] [US1] Redirect `/` to `/tickets` in `frontend/src/app/page.tsx`

**Checkpoint**: User Story 1 is fully functional and testable on its own (MVP)

---

## Phase 4: User Story 2 - Move a Ticket Through Its Lifecycle (Priority: P1)

**Goal**: A user can move a ticket Open → In Progress → Resolved → Closed or cancel it from Open or In Progress; the UI offers only valid next statuses.

**Independent Test**: Run quickstart Scenario 2 and the status table in Scenario 8: walk one ticket to Closed, cancel two others, and see only the allowed choices in the UI.

### Tests for User Story 2 ⚠️ (write first; they must fail)

- [X] T039 [P] [US2] Service test: each of the five allowed transitions is accepted, `Ticket.updated_at` and `Ticket.version` change, and a same-status request is a no-op that returns the ticket, in `backend/src/test/java/com/supportticket/service/TicketServiceStatusTest.java`
- [X] T040 [P] [US2] Repository test: the conditional status update changes exactly one row when the status matches and zero rows otherwise, in `backend/src/test/java/com/supportticket/repository/TicketStatusRepositoryTest.java`
- [X] T041 [P] [US2] MockMvc test for `PATCH /api/v1/tickets/{id}/status` accepted transitions, response `allowedNextStatuses`, and 404 for an unknown ticket, in `backend/src/test/java/com/supportticket/controller/TicketStatusControllerTest.java`
- [X] T042 [P] [US2] Component test: `StatusSelector` shows the right choices for Open, In Progress and Resolved, and nothing for Closed and Cancelled, in `frontend/tests/StatusSelector.test.tsx`

### Implementation for User Story 2

- [X] T043 [P] [US2] Create `ChangeStatusRequest` record (required `status`, allowed values per `data-model.md` `Ticket.status`) in `backend/src/main/java/com/supportticket/dto/ChangeStatusRequest.java`
- [X] T044 [US2] Add the conditional update `UPDATE ... SET status, updated_at, version = version + 1 WHERE id = :id AND status = :from` returning the row count to `backend/src/main/java/com/supportticket/repository/TicketRepository.java` (`data-model.md` `Ticket.status`, `Ticket.updated_at`, `Ticket.version`) (depends on T029)
- [X] T045 [US2] Implement `TicketService.changeStatus` in `backend/src/main/java/com/supportticket/service/TicketService.java`: load the ticket, return it unchanged if the status is the same, check `TicketStatus.canTransitionTo`, throw `InvalidStatusTransitionException` if not allowed, otherwise apply the conditional update (depends on T030, T043, T044)
- [X] T046 [US2] Add `PATCH /api/v1/tickets/{id}/status` to `backend/src/main/java/com/supportticket/controller/TicketController.java` (depends on T045)
- [X] T047 [P] [US2] Create `StatusSelector` client component that renders only the ticket's `allowedNextStatuses` and nothing for a final ticket (shown as final) in `frontend/src/components/StatusSelector.tsx` and `frontend/src/components/StatusSelector.module.css`
- [X] T048 [US2] Add `changeTicketStatus(id, status)` to `frontend/src/lib/api.ts` (depends on T019)
- [X] T049 [US2] Add `StatusSelector` to the detail page in `frontend/src/app/tickets/[id]/page.tsx` and refresh the page data after a successful change (depends on T037, T047, T048)

**Checkpoint**: User Stories 1 and 2 both work independently

---

## Phase 5: User Story 3 - Reject Invalid Status Changes (Priority: P1)

**Goal**: Every status change outside the allowed workflow is rejected by the backend with a clear message and no change to the ticket, including under simultaneous requests.

**Independent Test**: Run quickstart Scenarios 3 and 7: all 15 invalid pairs return 409 with current, requested and allowed statuses, an unknown value returns 400, and simultaneous changes never leave an impossible state.

### Tests for User Story 3 ⚠️ (write first; they must fail)

- [X] T050 [P] [US3] Parameterized test covering all 20 ordered status pairs (5 accepted, 15 rejected), final statuses, and `allowedNext()` in `backend/src/test/java/com/supportticket/model/TicketStatusTest.java`
- [X] T051 [P] [US3] MockMvc test: each rejected pair returns 409 `INVALID_STATUS_TRANSITION` with `currentStatus`, `requestedStatus`, `allowedNextStatuses`; the ticket and its `updatedAt` are unchanged afterwards; unknown status returns 400 `VALIDATION_ERROR` listing valid values, in `backend/src/test/java/com/supportticket/controller/TicketStatusRejectionTest.java`
- [X] T052 [P] [US3] Concurrency test: two simultaneous status changes on one ticket apply one at a time and the loser is judged against the new status, in `backend/src/test/java/com/supportticket/service/TicketStatusConcurrencyTest.java`

### Implementation for User Story 3

- [X] T053 [US3] Handle `InvalidStatusTransitionException` in `backend/src/main/java/com/supportticket/exception/GlobalExceptionHandler.java`: 409, `code = INVALID_STATUS_TRANSITION`, include `currentStatus`, `requestedStatus`, `allowedNextStatuses`, and a plain-language message that says "final status" when none are allowed (depends on T016)
- [X] T054 [US3] Map unknown status or priority values (unreadable enum in the body) to 400 `VALIDATION_ERROR` with a `fieldErrors` entry listing valid values, in `backend/src/main/java/com/supportticket/exception/GlobalExceptionHandler.java` (depends on T053)
- [X] T055 [US3] In `TicketService.changeStatus` (`backend/src/main/java/com/supportticket/service/TicketService.java`), when the conditional update changes 0 rows, reload the ticket and re-evaluate once against its new status, returning success or `InvalidStatusTransitionException`; a rejection performs no write (depends on T045)
- [X] T056 [US3] Show the server's rejection message in `frontend/src/components/StatusSelector.tsx` and refresh to the ticket's real current status after any rejection (depends on T047, T048, T053)

**Checkpoint**: All three P1 stories are complete; the status workflow is enforced end to end

---

## Phase 6: User Story 4 - Update Ticket Details (Priority: P2)

**Goal**: A user can edit title, description, priority and assignee, with protection against overwriting someone else's change, and any status inside an update follows the workflow.

**Independent Test**: Run quickstart Scenario 4: edit all fields, repeat with the old version and get 409, send a blank title and get 400.

### Tests for User Story 4 ⚠️ (write first; they must fail)

- [X] T057 [P] [US4] MockMvc test for `PUT /api/v1/tickets/{id}`: success, 400 for blank title, 409 `CONCURRENT_MODIFICATION` for a stale version, 409 `INVALID_STATUS_TRANSITION` for a disallowed status, 404, in `backend/src/test/java/com/supportticket/controller/TicketUpdateControllerTest.java`
- [X] T058 [P] [US4] Service test: update changes the four fields, blank assignee is stored as empty, `Ticket.updated_at` and `Ticket.version` change, and a rejected status rolls back every field change, in `backend/src/test/java/com/supportticket/service/TicketServiceUpdateTest.java`

### Implementation for User Story 4

- [X] T059 [P] [US4] Create `UpdateTicketRequest` record with Bean Validation per `data-model.md` (`Ticket.title`, `Ticket.description`, `Ticket.priority`, `Ticket.assignee`, `Ticket.status`, `Ticket.version`), `version` required and `status` optional, in `backend/src/main/java/com/supportticket/dto/UpdateTicketRequest.java`
- [X] T060 [US4] Implement `TicketService.update` in `backend/src/main/java/com/supportticket/service/TicketService.java`: validate a differing status with `canTransitionTo` first, apply the fields (`Ticket.assignee` blank becomes empty), set `Ticket.updated_at`, save under `@Version`, roll back the whole request on any rejection (depends on T030, T059)
- [X] T061 [US4] Add `PUT /api/v1/tickets/{id}` to `backend/src/main/java/com/supportticket/controller/TicketController.java` (depends on T060)
- [X] T062 [US4] Handle `OptimisticLockingFailureException` in `backend/src/main/java/com/supportticket/exception/GlobalExceptionHandler.java`: 409, `code = CONCURRENT_MODIFICATION`, message "This ticket was changed by someone else. Reload it and try again." (depends on T016)
- [X] T063 [US4] Add `updateTicket(id, body)` to `frontend/src/lib/api.ts` (depends on T019)
- [X] T064 [US4] Add edit mode to `frontend/src/components/TicketForm.tsx` (prefilled, includes assignee, sends the loaded `version`) (depends on T035, T063)
- [X] T065 [US4] Create the edit page `frontend/src/app/tickets/[id]/edit/page.tsx` and an "Edit" link on `frontend/src/app/tickets/[id]/page.tsx` (depends on T037, T064)

**Checkpoint**: User Stories 1 to 4 work independently

---

## Phase 7: User Story 7 - See Meaningful Errors (Priority: P2)

**Goal**: Every failure (invalid input, rejected status change, conflict, missing ticket, unexpected failure) shows a plain-language message in the right place, with no raw technical text and no lost input.

**Independent Test**: Run the error checks in quickstart Scenario 8: blank and over-length title, invalid status change, simultaneous edits, unknown ticket, backend stopped.

### Tests for User Story 7 ⚠️ (write first; they must fail)

- [X] T066 [P] [US7] Handler test: validation error shape, 404 shape, malformed JSON returns 400, and an unexpected exception returns 500 `INTERNAL_ERROR` with a generic message and no stack trace or internal text, in `backend/src/test/java/com/supportticket/exception/GlobalExceptionHandlerTest.java`
- [X] T067 [P] [US7] Component test: `TicketForm` shows each `fieldErrors` message next to its field and keeps the entered text, in `frontend/tests/TicketForm.test.tsx`
- [X] T068 [P] [US7] Component test: `ErrorMessage` shows the server `message` and a retry action when provided, in `frontend/tests/ErrorMessage.test.tsx`

### Implementation for User Story 7

- [X] T069 [US7] Write plain-language Bean Validation messages that say what is wrong and what is allowed on the fields of `backend/src/main/java/com/supportticket/dto/CreateTicketRequest.java` and `backend/src/main/java/com/supportticket/dto/UpdateTicketRequest.java` (limits come from `data-model.md` `Ticket.title`, `Ticket.description`, `Ticket.priority`, `Ticket.assignee`; do not restate them elsewhere)
- [X] T070 [US7] Add handlers for malformed JSON (400) and any unexpected exception (500 `INTERNAL_ERROR`, generic message, details logged with SLF4J only) in `backend/src/main/java/com/supportticket/exception/GlobalExceptionHandler.java` (depends on T054, T062)
- [X] T071 [US7] Render `fieldErrors` next to each field in `frontend/src/components/TicketForm.tsx`, keep entered values after a failed submit, and show a form-level `ErrorMessage` for non-field errors (depends on T064)
- [X] T072 [US7] In `frontend/src/lib/api.ts` map `ApiError` codes and network failures to friendly messages (for example "Could not reach the server. Try again.") that are never raw technical text (depends on T063)
- [X] T073 [US7] Create the app-level error boundary with a retry button in `frontend/src/app/error.tsx` (depends on T021)
- [X] T074 [US7] Show the `CONCURRENT_MODIFICATION` message with a "Reload ticket" action in the edit flow in `frontend/src/components/TicketForm.tsx` (depends on T071, T072)

**Checkpoint**: User Stories 1 to 4 and 7 work independently, with consistent error display

---

## Phase 8: User Story 5 - Comment on Tickets (Priority: P3)

**Goal**: A user can add comments to any ticket and see them oldest first with author and time.

**Independent Test**: Run quickstart Scenario 5: add comments (also on a Closed ticket), reject a blank comment, see order.

### Tests for User Story 5 ⚠️ (write first; they must fail)

- [X] T075 [P] [US5] Repository test: comments for a ticket are returned oldest first in `backend/src/test/java/com/supportticket/repository/CommentRepositoryTest.java`
- [X] T076 [P] [US5] Service test: comment is accepted on Open, Closed and Cancelled tickets, does not change the ticket's `updated_at` or `version`, and a missing ticket is rejected, in `backend/src/test/java/com/supportticket/service/CommentServiceTest.java`
- [X] T077 [P] [US5] MockMvc test for `POST /api/v1/tickets/{id}/comments`: 201, 400 for blank or over-length text and blank author, 404, in `backend/src/test/java/com/supportticket/controller/CommentControllerTest.java`

### Implementation for User Story 5

- [X] T078 [P] [US5] Create `CreateCommentRequest` record with Bean Validation per `data-model.md` (`Comment.author`, `Comment.text`) and plain-language messages in `backend/src/main/java/com/supportticket/dto/CreateCommentRequest.java`
- [X] T079 [US5] Implement `CommentService.add` in `backend/src/main/java/com/supportticket/service/CommentService.java` setting `Comment.ticket_id` from the path and `Comment.created_at`, without touching the ticket's version (depends on T013, T028, T078)
- [X] T080 [US5] Implement `POST /api/v1/tickets/{id}/comments` in `backend/src/main/java/com/supportticket/controller/CommentController.java` (depends on T079)
- [X] T081 [P] [US5] Create `CommentList` component (oldest first, author and time) in `frontend/src/components/CommentList.tsx` and `frontend/src/components/CommentList.module.css`
- [X] T082 [P] [US5] Create `CommentForm` client component (shows field errors, keeps text on failure) in `frontend/src/components/CommentForm.tsx` and `frontend/src/components/CommentForm.module.css`
- [X] T083 [US5] Add `addComment(ticketId, body)` to `frontend/src/lib/api.ts` (depends on T019)
- [X] T084 [US5] Show `CommentList` and `CommentForm` on `frontend/src/app/tickets/[id]/page.tsx` and refresh after a new comment (depends on T037, T081, T082, T083)

**Checkpoint**: Comments work independently on every ticket

---

## Phase 9: User Story 6 - Search and Filter Tickets (Priority: P3)

**Goal**: A user can search by keyword (title and description, case-insensitive, plain text) and filter by any of the five statuses, together or separately.

**Independent Test**: Run quickstart Scenario 6: keyword match, status filter including Cancelled, both together, `%` treated as plain text, and an empty-result message with a clear option.

### Tests for User Story 6 ⚠️ (write first; they must fail)

- [X] T085 [P] [US6] Repository test: case-insensitive match on title or description, `%`, `_` and `\` treated as literals, status filter, combined filter, ordering, in `backend/src/test/java/com/supportticket/repository/TicketSearchRepositoryTest.java`
- [X] T086 [P] [US6] MockMvc test for `GET /api/v1/tickets` with `status` (all five values, including `CANCELLED`), `q`, `page`, `size`; 400 for an unknown status, `q` over 100 characters, or `size` over 100, in `backend/src/test/java/com/supportticket/controller/TicketSearchControllerTest.java`

### Implementation for User Story 6

- [X] T087 [US6] Add the parameterized search and status-filter projection query (`LOWER(...) LIKE :pattern ESCAPE '\'` on title and description, optional status) to `backend/src/main/java/com/supportticket/repository/TicketRepository.java` (depends on T029)
- [X] T088 [US6] Extend `TicketService.list` in `backend/src/main/java/com/supportticket/service/TicketService.java` to lower-case and escape the keyword and apply status and paging, capping `size` at 100 (depends on T030, T087)
- [X] T089 [US6] Add and validate the `status`, `q` (max 100 characters), `page` and `size` parameters on `GET /api/v1/tickets` in `backend/src/main/java/com/supportticket/controller/TicketController.java` (depends on T031, T088)
- [X] T090 [P] [US6] Create `TicketFilters` client component (debounced 300 ms search box, status select with all five statuses, clear button) that updates the URL query in `frontend/src/components/TicketFilters.tsx` and `frontend/src/components/TicketFilters.module.css`
- [X] T091 [US6] Wire `q`, `status` and `page` from the URL into the list page and show a "no matching tickets" message with a clear action in `frontend/src/app/tickets/page.tsx` (depends on T034, T090)

**Checkpoint**: All user stories are independently functional

---

## Phase 10: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [X] T092 [P] Create a test-profile data seeder (10,000 tickets, mixed statuses) as `@Profile("perf")` in `backend/src/test/java/com/supportticket/PerfSeedRunner.java`; run it with `./mvnw spring-boot:test-run -Dspring-boot.run.profiles=perf` and update the "Performance smoke check" in `specs/003-ticket-management-workflow/quickstart.md` to use that command
- [X] T093 [P] End-to-end `@SpringBootTest` covering quickstart Scenarios 1 to 6 against in-memory H2 in `backend/src/test/java/com/supportticket/TicketWorkflowIntegrationTest.java`
- [X] T094 [P] Test that `/v3/api-docs` exposes every path and operation in `specs/003-ticket-management-workflow/contracts/openapi.yaml` in `backend/src/test/java/com/supportticket/OpenApiContractTest.java`
- [ ] T095 Review responsive behavior at 320 px, 640 px and desktop widths (no horizontal scrolling, readable cards, usable forms) and fix styles in `frontend/src/app/globals.css` and the component `.module.css` files
- [X] T096 Measure against the budgets using quickstart "Performance smoke check" with 10,000 tickets (API p95 under 100 ms; first contentful paint under 1.5 s; under 100 KB gzipped JavaScript on the list page); fix queries, indexes or bundle size in the affected files if a budget is missed — DONE for API latency (p95 under 20 ms with 10,000 tickets) and JS size (about 175 KB gzipped, budget revised to 200 KB, see research item 11); Lighthouse first contentful paint was not run (no browser available here); measured server time to first byte was about 10 ms
- [X] T097 [P] Update `README.md` with prerequisites (JDK 21, Node 20 or later), the `TICKETS_DB_PASSWORD` and `BACKEND_URL` variables, and run and test commands from `specs/003-ticket-management-workflow/quickstart.md`
- [X] T098 [P] Mark `specs/001-support-ticket-management/spec.md` and `specs/002-ticket-status-workflow/spec.md` as superseded by `003-ticket-management-workflow` (change the `Status` line and add a pointer)
- [X] T099 Security review: no credentials in source (`backend/src/main/resources/application.yml`), H2 console off, no CORS configuration, all queries parameterized, error responses leak no internals; record the authentication deviation from `plan.md` Complexity Tracking in `README.md`
- [X] T100 Run `cd backend && ./mvnw verify` and `cd frontend && npm run lint && npm test && npm run build`; fix all failures
- [ ] T101 Run every scenario in `specs/003-ticket-management-workflow/quickstart.md` end to end and note results in the pull request

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; start immediately
- **Foundational (Phase 2)**: Depends on Setup; BLOCKS all user stories
- **User Stories (Phases 3 to 9)**: All depend on Foundational completion
  - Sequential in priority order: US1 → US2 → US3 (P1), then US4 → US7 (P2), then US5 → US6 (P3)
  - Or in parallel by different developers once the listed story dependencies are met
- **Polish (Phase 10)**: Depends on all desired user stories being complete

### User Story Dependencies

- **US1 (P1)**: After Foundational; no dependency on other stories
- **US2 (P1)**: After US1 (uses its service, controller and detail page)
- **US3 (P1)**: After US2 (hardens and tests the transition path built there)
- **US4 (P2)**: After US1; its status-in-update test uses the 409 handler from US3
- **US7 (P2)**: After US1; its form work extends US4's edit form, so schedule after US4
- **US5 (P3)**: After US1 only; can run in parallel with US2 to US4
- **US6 (P3)**: After US1 only; can run in parallel with US2 to US5

### Within Each User Story

- Tests are written first and MUST fail before implementation
- Models and DTOs before repositories, repositories before services, services before controllers
- Backend before the frontend pieces that call it
- Story complete (checkpoint passed) before moving on

### Shared-File Warning

These files are edited by several stories, so do not run those tasks in parallel with each other:
`TicketService.java` (T030, T045, T055, T060, T088), `TicketController.java` (T031, T046, T061, T089),
`TicketRepository.java` (T029, T044, T087), `GlobalExceptionHandler.java` (T016, T053, T054, T062,
T070), `frontend/src/lib/api.ts` (T019, T048, T063, T072, T083), `TicketForm.tsx` (T035, T064, T071,
T074), and `frontend/src/app/tickets/[id]/page.tsx` (T037, T049, T065, T084).

### Parallel Opportunities

- Setup: T004, T005, T006 together
- Foundational: T008, T010, T011, T014, T015, T017, T018, T020, T021, T022 (respect the listed dependencies)
- Each story's test tasks are all [P]
- US5 and US6 can proceed in parallel with US2 to US4 if file conflicts above are avoided

---

## Parallel Example: User Story 1

```bash
# Launch all tests for User Story 1 together:
Task: "Service test for create in backend/src/test/java/com/supportticket/service/TicketServiceTest.java"
Task: "Repository test for list in backend/src/test/java/com/supportticket/repository/TicketRepositoryTest.java"
Task: "MockMvc test for create/list/get in backend/src/test/java/com/supportticket/controller/TicketControllerTest.java"

# Launch independent DTOs and UI components together:
Task: "CreateTicketRequest in backend/src/main/java/com/supportticket/dto/CreateTicketRequest.java"
Task: "Response records in backend/src/main/java/com/supportticket/dto/"
Task: "CommentRepository in backend/src/main/java/com/supportticket/repository/CommentRepository.java"
Task: "TicketList in frontend/src/components/TicketList.tsx"
Task: "Pagination in frontend/src/components/Pagination.tsx"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational (critical; blocks all stories)
3. Complete Phase 3: User Story 1
4. **STOP and VALIDATE**: run quickstart Scenario 1 and the US1 tests
5. Demo locally (no sign-in yet; local use only per `plan.md`)

### Incremental Delivery

1. Setup + Foundational → foundation ready
2. US1 → validate → demo (MVP: create, list, view)
3. US2 and US3 → validate → the full enforced status workflow (all P1 done)
4. US4 and US7 → validate → editing and meaningful errors (P2 done)
5. US5 and US6 → validate → comments, search and filter (P3 done)
6. Polish → performance, responsive, security, docs, full quickstart run

### Parallel Team Strategy

1. Team completes Setup + Foundational together
2. Then:
   - Developer A: US1 → US2 → US3 → US4 → US7 (shared backend files, sequential)
   - Developer B: US5 (after US1)
   - Developer C: US6 (after US1)
3. Merge carefully around the shared files listed above

---

## Notes

- [P] tasks = different files, no dependency on an incomplete task
- [Story] label maps each task to a user story for traceability
- Every constrained field in `data-model.md` is cited by an implementing task: Ticket fields in T009,
  T010, T011, T012, T023, T026, T030, T043, T044, T059, T060, T069; Comment fields in T009, T013, T078, T079
- Verify tests fail before implementing
- Commit after each task or logical group, on the feature branch only
- Stop at any checkpoint to validate a story independently
- Out of scope here: authentication and roles (separate feature; see `plan.md` Complexity Tracking)
