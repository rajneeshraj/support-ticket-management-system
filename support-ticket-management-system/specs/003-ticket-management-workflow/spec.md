# Feature Specification: Ticket Management with Status Workflow

**Feature Branch**: `003-ticket-management-workflow`

**Created**: 2026-11-22

**Status**: Draft

**Input**: User description: "Build a Support Ticket Management System with the following features: Create a ticket, List tickets, View ticket details, Update title, description, priority and assignee, Add comments, Search tickets by keyword, Filter tickets by status, Persist data in a database, Validate input at the backend, Display meaningful errors in the UI. The following state machine must be enforced by the backend: OPEN → IN_PROGRESS → RESOLVED → CLOSED; OPEN → CANCELLED; IN_PROGRESS → CANCELLED. Invalid transitions must be rejected. Example: CLOSED → OPEN ❌, RESOLVED → OPEN ❌, CANCELLED → OPEN ❌"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create, List and View Tickets (Priority: P1)

A support user raises a new ticket by entering a title, a description and a priority. The ticket is
saved, appears in the ticket list, and can be opened to see all of its details. This is the core of
the system: without it, no other capability has any value.

**Why this priority**: Creating, listing and viewing tickets is the minimum viable product. Every
other story builds on tickets existing and being visible.

**Independent Test**: Can be fully tested by creating a ticket, confirming it appears in the list,
opening it, and confirming the details match what was entered. Delivers a usable ticket log.

**Acceptance Scenarios**:

1. **Given** the user is on the new-ticket form, **When** they enter a valid title, description and
   priority and submit, **Then** the ticket is saved with status Open and appears in the ticket list.
2. **Given** a ticket is created, **When** it is saved, **Then** it always starts as Open and has no
   assignee, even if a different status was supplied.
3. **Given** tickets exist, **When** the user opens the ticket list, **Then** each ticket shows its
   title, status, priority, assignee and creation date, newest first.
4. **Given** a ticket exists, **When** the user selects it, **Then** they see its full details:
   title, description, status, priority, assignee, created and last-updated dates, and comments.
5. **Given** no tickets exist, **When** the user opens the list, **Then** they see a clear "no
   tickets yet" message.
6. **Given** the user opens a ticket that does not exist, **When** the page loads, **Then** they see
   a "ticket not found" message with a way back to the list.

---

### User Story 2 - Move a Ticket Through Its Lifecycle (Priority: P1)

A support user progresses a ticket through its normal lifecycle: Open, then In Progress when work
starts, then Resolved when the fix is delivered, then Closed once confirmed. A ticket that no longer
needs work can be Cancelled, but only while it is Open or In Progress.

**Why this priority**: Status is the heart of ticket handling. Tickets cannot reflect real progress
or be filtered meaningfully without it.

**Independent Test**: Can be fully tested by moving one ticket Open → In Progress → Resolved →
Closed and cancelling two others (one Open, one In Progress), confirming each change is accepted
and shown.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, **When** the user moves it to In Progress, **Then** the change is
   accepted and the ticket shows In Progress.
2. **Given** a ticket is In Progress, **When** the user moves it to Resolved, **Then** the change is
   accepted and the ticket shows Resolved.
3. **Given** a ticket is Resolved, **When** the user moves it to Closed, **Then** the change is
   accepted and the ticket shows Closed.
4. **Given** a ticket is Open, **When** the user cancels it, **Then** it becomes Cancelled.
5. **Given** a ticket is In Progress, **When** the user cancels it, **Then** it becomes Cancelled.
6. **Given** any accepted status change, **When** it is saved, **Then** the ticket's last-updated
   time is refreshed.
7. **Given** a ticket is Open, In Progress or Resolved, **When** the user views its status choices,
   **Then** only the statuses allowed next are offered (Open: In Progress, Cancelled; In Progress:
   Resolved, Cancelled; Resolved: Closed).
8. **Given** a ticket is Closed or Cancelled, **When** the user views it, **Then** no status change
   is offered and the ticket is shown as final.

---

### User Story 3 - Reject Invalid Status Changes (Priority: P1)

The system refuses any status change that is not part of the allowed workflow, no matter which
client or screen requested it, and tells the user exactly why. A rejected change leaves the ticket
unchanged.

**Why this priority**: Enforcing the workflow is a stated must-have. If invalid changes can slip
through, ticket statuses cannot be trusted.

**Independent Test**: Can be fully tested by attempting every status change that is not in the
allowed list and confirming each is rejected with a clear message and no change to the ticket.

**Acceptance Scenarios**:

1. **Given** a ticket is Closed, **When** a change to Open is requested, **Then** it is rejected.
2. **Given** a ticket is Resolved, **When** a change to Open is requested, **Then** it is rejected.
3. **Given** a ticket is Cancelled, **When** a change to Open is requested, **Then** it is rejected.
4. **Given** a ticket is Open, **When** a change straight to Resolved or Closed is requested
   (skipping a step), **Then** it is rejected.
5. **Given** a ticket is In Progress, **When** a change back to Open is requested, **Then** it is
   rejected.
6. **Given** a ticket is Resolved, **When** a change to In Progress or Cancelled is requested,
   **Then** it is rejected.
7. **Given** a ticket is Closed or Cancelled, **When** any change to a different status is
   requested, **Then** it is rejected because those statuses are final.
8. **Given** any rejected status change, **When** the response is shown, **Then** it states the
   current status, the requested status, and the statuses allowed next, and the ticket's status and
   last-updated time are unchanged.

---

### User Story 4 - Update Ticket Details (Priority: P2)

A support user edits an existing ticket to change its title, description, priority or assignee as
the situation changes.

**Why this priority**: Tickets are only useful if they can be kept accurate and routed to the right
person. It depends on Story 1.

**Independent Test**: Can be fully tested by editing each field of an existing ticket, saving, and
confirming the new values appear on the detail page and in the list.

**Acceptance Scenarios**:

1. **Given** a ticket exists, **When** the user changes its title, description, priority or assignee
   and saves, **Then** the updated values are shown on the detail page and in the list, and the
   last-updated time changes.
2. **Given** the user clears the title while editing, **When** they try to save, **Then** the save is
   rejected and the original values are kept.
3. **Given** a ticket was changed by someone else after the user opened it, **When** the user saves,
   **Then** they are told the ticket changed and cannot silently overwrite the other change.
4. **Given** an edit includes a status change, **When** it is saved, **Then** the status change must
   follow the same workflow rules as in Story 3.

---

### User Story 5 - Comment on Tickets (Priority: P3)

A support user adds comments to a ticket to record progress, questions and decisions. Comments are
shown on the ticket in the order they were added.

**Why this priority**: Comments give tickets a history and enable collaboration, but a ticket is
still usable without them.

**Independent Test**: Can be fully tested by adding several comments to a ticket and confirming they
appear in chronological order with author and time.

**Acceptance Scenarios**:

1. **Given** a ticket is open on the detail page, **When** the user submits a non-empty comment,
   **Then** it appears at the end of the comment list with its author and timestamp.
2. **Given** a ticket has comments, **When** the user opens it, **Then** all comments are shown
   oldest first.
3. **Given** the user submits an empty or whitespace-only comment, **When** they try to post it,
   **Then** it is rejected with a clear message and nothing is added.
4. **Given** a ticket is Closed or Cancelled, **When** the user adds a comment, **Then** it is
   accepted, because only status is restricted on final tickets.

---

### User Story 6 - Search and Filter Tickets (Priority: P3)

A support user finds tickets quickly by typing a keyword (matched against title and description) and
by filtering the list to one status: Open, In Progress, Resolved, Closed or Cancelled.

**Why this priority**: Search and filtering make the list usable as it grows, but only matter once
there are enough tickets to need them.

**Independent Test**: Can be fully tested with tickets of varying statuses and text: searching for a
word returns only tickets containing it, and filtering by status returns only tickets in that
status.

**Acceptance Scenarios**:

1. **Given** tickets exist, **When** the user searches for a keyword, **Then** only tickets whose
   title or description contain it (ignoring upper/lower case) are listed.
2. **Given** tickets exist in several statuses, **When** the user filters by a status, **Then** only
   tickets in that status are listed, including the Cancelled filter.
3. **Given** a keyword search and a status filter are both applied, **When** the list is shown,
   **Then** only tickets matching both are listed.
4. **Given** nothing matches, **When** the list is shown, **Then** the user sees a "no matching
   tickets" message and can clear the search and filter.

---

### User Story 7 - See Meaningful Errors (Priority: P2)

When something goes wrong, whether invalid input, a rejected status change, a missing ticket or an
unexpected failure, the user sees a clear, plain-language message that explains what happened and
what to do, and never raw technical text.

**Why this priority**: Clear errors let users correct mistakes themselves and are required for the
validation and workflow rules to be usable.

**Independent Test**: Can be fully tested by triggering each kind of failure (blank title,
over-length text, invalid status change, unknown ticket, unavailable service) and checking the
message shown.

**Acceptance Scenarios**:

1. **Given** the user submits invalid input, **When** the system rejects it, **Then** each invalid
   field is highlighted with a message saying what is wrong and what is allowed.
2. **Given** a status change is rejected, **When** the user sees the result, **Then** the system's
   explanation is shown in plain language and the screen refreshes to the ticket's real current
   status.
3. **Given** the service is unreachable or fails unexpectedly, **When** the user acts, **Then** they
   see a friendly message with the option to retry, and text they entered is not lost.

---

### Edge Cases

- Title is empty, whitespace-only, or too long: rejected with a field-specific message.
- Description or comment is empty or too long: rejected with a field-specific message.
- Priority is not one of the allowed values, or status is not one of the five known values: rejected
  as invalid input listing the valid values. This is a different message from an invalid status
  change between two known statuses.
- A status change requests the ticket's current status (for example Open to Open): it is not a
  transition and is not rejected; other edits in the same request still apply.
- Two users change the same ticket's status at the same time: only the first is applied; the second
  is checked against the new status and is accepted or rejected with a clear message.
- Two users edit the same ticket's details at once: the later save is refused with a "changed by
  someone else" message.
- A keyword contains special characters (for example `%`, quotes or `<script>`): it is treated as
  plain text and never breaks the search or the page.
- A new ticket is submitted with a status other than Open: the supplied status is ignored and the
  ticket is created as Open.
- Very large lists: results are shown in pages so the list stays responsive.
- Data entered must still be there after the system is restarted.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to create a ticket by providing a title, a description and a
  priority.
- **FR-002**: The system MUST give every new ticket a unique identifier, the status Open, no
  assignee, and a creation time, regardless of any status supplied by the user.
- **FR-003**: Users MUST be able to view a list of tickets showing title, status, priority,
  assignee and creation date, newest first and in pages.
- **FR-004**: Users MUST be able to open one ticket and see all its details, including comments.
- **FR-005**: Users MUST be able to update a ticket's title, description, priority and assignee.
- **FR-006**: The system MUST record and show a last-updated time that changes whenever a ticket is
  edited or its status changes.
- **FR-007**: Users MUST be able to add a comment to a ticket; each comment MUST store its text,
  author and time and be shown oldest first.
- **FR-008**: Users MUST be able to search tickets by keyword, matched case-insensitively against
  title and description, treating all characters as plain text.
- **FR-009**: Users MUST be able to filter tickets by each of the five statuses and combine the
  filter with a keyword search.
- **FR-010**: The system MUST persist all tickets and comments so they survive a restart.
- **FR-011**: The system MUST support exactly five ticket statuses: Open, In Progress, Resolved,
  Closed and Cancelled.
- **FR-012**: The system MUST allow only these status transitions, and no others:
  - Open → In Progress
  - In Progress → Resolved
  - Resolved → Closed
  - Open → Cancelled
  - In Progress → Cancelled
- **FR-013**: The system MUST treat Closed and Cancelled as final; a ticket in a final status MUST
  NOT move to any other status.
- **FR-014**: The system MUST enforce FR-012 and FR-013 itself, so that no screen, client or direct
  request can bypass them, including any update request that carries a status.
- **FR-015**: The system MUST reject every transition not listed in FR-012, including at least:
  Closed → Open, Resolved → Open, Cancelled → Open, In Progress → Open, Open → Resolved,
  Open → Closed, In Progress → Closed, Resolved → In Progress and Resolved → Cancelled.
- **FR-016**: A rejected transition MUST leave the ticket completely unchanged, including its status
  and last-updated time.
- **FR-017**: A rejection MUST state the current status, the requested status, and the statuses
  allowed next (or that the status is final).
- **FR-018**: A request that keeps the ticket's current status MUST NOT be treated as a transition
  and MUST NOT be rejected for that reason.
- **FR-019**: When two status changes for the same ticket arrive at the same time, the system MUST
  apply them one at a time, checking each against the ticket's status at that moment.
- **FR-020**: The system MUST make each ticket's allowed next statuses available so screens can offer
  only valid choices.
- **FR-021**: The system MUST validate all input on the server side, independent of any checks made
  in the user interface:
  - title: required, not blank, maximum 150 characters;
  - description: required, not blank, maximum 5,000 characters;
  - comment: required, not blank, maximum 2,000 characters;
  - priority: one of Low, Medium, High, Critical;
  - status: one of Open, In Progress, Resolved, Closed, Cancelled.
- **FR-022**: When validation fails, the system MUST reject the request without changing any data and
  identify each invalid field and the reason. An unknown status value MUST be reported as invalid
  input, separately from an invalid transition.
- **FR-023**: The system MUST prevent a user from overwriting a ticket that was changed by someone
  else since they loaded it, and MUST tell them so.
- **FR-024**: The user interface MUST show validation and rejection messages next to the relevant
  fields or controls in plain language, MUST offer only the allowed next statuses and none for final
  tickets, and MUST show a clear message for "not found" and unexpected failures.

### Key Entities

- **Ticket**: A support request. Has a unique identifier, title, description, status, priority, an
  optional assignee, created and last-updated times, and zero or more comments.
- **Ticket Status**: The stage a ticket is in: Open, In Progress, Resolved, Closed or Cancelled.
  Closed and Cancelled are final.
- **Status Transition Rule**: A permitted move from one status to another. The complete set is
  defined in FR-012; any other move between different statuses is invalid.
- **Comment**: A note added to one ticket. Has text, author and a creation time. Belongs to exactly
  one ticket.
- **Assignee**: The person responsible for a ticket, identified by name. A ticket has at most one
  assignee, and may have none.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create a ticket and see it in the list in under 1 minute.
- **SC-002**: A user can find a specific ticket by keyword or status filter in under 30 seconds in a
  list of 1,000 tickets.
- **SC-003**: The ticket list, ticket details, search results and the outcome of a status change
  (accepted or rejected) appear within 2 seconds for 95% of requests with up to 1,000 tickets.
- **SC-004**: 100% of the five allowed transitions are accepted from the correct starting status.
- **SC-005**: 100% of the 15 other status changes between different statuses are rejected, with no
  change to the ticket.
- **SC-006**: 100% of rejection messages name the current status, the requested status, and the
  allowed next statuses or say the status is final.
- **SC-007**: 100% of invalid submissions (blank or over-length fields, unknown priority or status)
  are rejected without changing stored data, and the user sees a message naming the problem field
  and reason with no raw technical text.
- **SC-008**: 100% of tickets, comments and statuses created before a restart are still present
  afterwards.
- **SC-009**: Users are never offered a status choice that the system would reject.
- **SC-010**: 90% of first-time users complete "create a ticket", "change its status" and "add a
  comment" without help.

## Assumptions

- This feature is the combined, self-contained specification of the full Support Ticket Management
  system. It **supersedes** `specs/001-support-ticket-management` (which allowed any status to move
  to any other and had four statuses) and **includes** the scope of `specs/002-ticket-status-workflow`.
  The three specs overlap; keep one as the source of truth and mark the others as superseded.
- Users are internal support staff; customer self-service is out of scope.
- Sign-in, user accounts and role-based permissions are handled by a separate feature. For now every
  user is treated as a permitted support user, and comment author and assignee are entered as plain
  names. This conflicts with the project constitution's requirement for authentication on all
  endpoints other than login and health, and must be resolved before any non-local use.
- Statuses are shown to users as Open, In Progress, Resolved, Closed and Cancelled; the canonical
  names are OPEN, IN_PROGRESS, RESOLVED, CLOSED and CANCELLED.
- Closed and Cancelled are final: tickets cannot be re-opened; a new ticket is raised instead.
- Only status is restricted on final tickets. Editing title, description, priority and assignee, and
  adding comments, remain allowed on them; locking final tickets is out of scope.
- Priorities are the fixed set Low, Medium, High and Critical; Medium is the default if none is
  chosen.
- Out of scope: deleting tickets or comments, editing or deleting comments, attachments,
  notifications, SLAs, reporting, cancellation reasons, and status-change history or audit trails.
- Keyword search is a simple "contains" match on title and description; ranking and fuzzy matching
  are out of scope.
- Times are shown in the user's local time zone and stored consistently for all users.
- Ticket lists show 20 tickets per page by default.
- The system is used in a modern desktop or mobile web browser; native apps are out of scope.
