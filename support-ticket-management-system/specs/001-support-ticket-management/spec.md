# Feature Specification: Support Ticket Management

**Feature Branch**: `001-support-ticket-management`

**Created**: 2026-11-22

**Status**: Superseded by [003-ticket-management-workflow](../003-ticket-management-workflow/spec.md), which merges this feature with 002.

**Input**: User description: "Build a Support Ticket Management System with the following features: Create a ticket, List tickets, View ticket details, Update title, description, priority and assignee, Add comments, Search tickets by keyword, Filter tickets by status, Persist data in a database, Validate input at the backend, Display meaningful errors in the UI."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Create and View Tickets (Priority: P1)

A support user raises a new ticket by entering a title, a description and a priority. The ticket is
saved, appears in the ticket list, and can be opened to see all of its details. This is the core of
the system: without it, no other capability has any value.

**Why this priority**: Creating, listing and viewing tickets is the minimum viable product. Every
other story builds on tickets existing and being visible.

**Independent Test**: Can be fully tested by creating a ticket, confirming it appears in the list,
opening it, and confirming the details match what was entered. Delivers a usable ticket log.

**Acceptance Scenarios**:

1. **Given** the user is on the new-ticket form, **When** they enter a valid title, description and
   priority and submit, **Then** the ticket is saved with status "Open" and the user sees it in the
   ticket list.
2. **Given** tickets exist, **When** the user opens the ticket list, **Then** each ticket shows its
   title, status, priority, assignee and creation date, with the newest tickets first.
3. **Given** a ticket exists, **When** the user selects it from the list, **Then** they see its full
   details: title, description, status, priority, assignee, created and last-updated dates, and
   comments.
4. **Given** no tickets exist, **When** the user opens the ticket list, **Then** they see a clear
   "no tickets yet" message rather than an empty or broken screen.
5. **Given** the user tries to open a ticket that does not exist, **When** the page loads, **Then**
   they see a "ticket not found" message with a way back to the list.

---

### User Story 2 - Update Ticket Details (Priority: P2)

A support user edits an existing ticket to change its title, description, priority or assignee, and
to move it through its status (Open, In Progress, Resolved, Closed) as work progresses.

**Why this priority**: Tickets are only useful if they can be kept accurate and routed to the right
person as the situation changes. It depends on Story 1.

**Independent Test**: Can be fully tested by editing each field of an existing ticket, saving, and
confirming the new values are shown on the detail page and in the list.

**Acceptance Scenarios**:

1. **Given** a ticket exists, **When** the user changes its title, description, priority or assignee
   and saves, **Then** the updated values are shown on the detail page and in the list, and the
   last-updated date changes.
2. **Given** a ticket exists, **When** the user changes its status, **Then** the new status is shown
   and the ticket appears under the matching status filter.
3. **Given** the user clears the title while editing, **When** they try to save, **Then** the save is
   rejected and the original values are kept.
4. **Given** a ticket was changed by someone else after the user opened it, **When** the user saves
   their edit, **Then** they are told the ticket changed and are not allowed to silently overwrite
   the other change.

---

### User Story 3 - Comment on Tickets (Priority: P3)

A support user adds comments to a ticket to record progress, questions and decisions. Comments are
shown on the ticket details in the order they were added.

**Why this priority**: Comments give tickets a history and enable collaboration, but a ticket is
still usable without them.

**Independent Test**: Can be fully tested by adding several comments to a ticket and confirming they
appear on the detail page in chronological order with author and time.

**Acceptance Scenarios**:

1. **Given** a ticket is open on the detail page, **When** the user submits a non-empty comment,
   **Then** the comment appears at the end of the comment list with its author and timestamp.
2. **Given** a ticket has existing comments, **When** the user opens it, **Then** all comments are
   shown oldest first.
3. **Given** the user submits an empty or whitespace-only comment, **When** they try to post it,
   **Then** it is rejected with a clear message and nothing is added.

---

### User Story 4 - Search and Filter Tickets (Priority: P4)

A support user finds tickets quickly by typing a keyword (matched against ticket title and
description) and by filtering the list to a single status such as "Open".

**Why this priority**: Search and filtering make the list usable as it grows, but they are only
valuable once there are enough tickets to need them.

**Independent Test**: Can be fully tested with a set of tickets of varying statuses and text:
searching for a word returns only tickets containing it, and filtering by status returns only
tickets in that status.

**Acceptance Scenarios**:

1. **Given** tickets exist, **When** the user searches for a keyword, **Then** only tickets whose
   title or description contain that keyword (ignoring upper/lower case) are listed.
2. **Given** tickets exist in several statuses, **When** the user filters by a status, **Then** only
   tickets in that status are listed.
3. **Given** a keyword search and a status filter are both applied, **When** the list is shown,
   **Then** only tickets matching both are listed.
4. **Given** no ticket matches the search or filter, **When** the list is shown, **Then** the user
   sees a "no matching tickets" message and can clear the search and filter.

---

### Edge Cases

- Title is empty, whitespace-only, or longer than the allowed length: the ticket is rejected and the
  user is told which field is wrong and why.
- Description is empty or longer than the allowed length: rejected with a field-specific message.
- Priority or status is not one of the allowed values: rejected with a message listing valid values.
- The backend is unreachable or returns an unexpected failure: the user sees a plain-language error
  with the option to retry, and any text they entered is not lost.
- The same ticket is edited by two users at once: the later save is refused with a "changed by
  someone else" message (see Story 2).
- A search keyword contains special characters (for example `%`, quotes or `<script>`): it is treated
  as plain text and never breaks the search or the page.
- Very large lists: the list is shown in pages so it stays responsive.
- Data entered must still be there after the system is restarted.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: Users MUST be able to create a ticket by providing a title, a description and a
  priority.
- **FR-002**: The system MUST give every new ticket a unique identifier, the status "Open", an
  unassigned assignee, and a creation timestamp.
- **FR-003**: Users MUST be able to view a list of tickets showing title, status, priority,
  assignee and creation date, ordered newest first and shown in pages.
- **FR-004**: Users MUST be able to open a single ticket and see all its details, including its
  comments.
- **FR-005**: Users MUST be able to update a ticket's title, description, priority and assignee.
- **FR-006**: Users MUST be able to change a ticket's status among Open, In Progress, Resolved and
  Closed.
- **FR-007**: The system MUST record and show a last-updated timestamp that changes whenever a
  ticket is edited.
- **FR-008**: Users MUST be able to add a comment to a ticket; each comment MUST store its text,
  author and timestamp and be shown oldest first.
- **FR-009**: Users MUST be able to search tickets by keyword, matched case-insensitively against
  title and description.
- **FR-010**: Users MUST be able to filter tickets by status, and combine the filter with a keyword
  search.
- **FR-011**: The system MUST persist all tickets and comments so they survive a system restart.
- **FR-012**: The system MUST validate all input on the server side, independent of any checks made
  in the user interface:
  - title: required, not blank, maximum 150 characters;
  - description: required, not blank, maximum 5,000 characters;
  - comment: required, not blank, maximum 2,000 characters;
  - priority: one of Low, Medium, High, Critical;
  - status: one of Open, In Progress, Resolved, Closed.
- **FR-013**: When validation fails, the system MUST reject the request without changing any data and
  return a message identifying each invalid field and the reason.
- **FR-014**: The user interface MUST display the error messages from FR-013 next to the relevant
  fields, and MUST display a clear, non-technical message for "not found" and unexpected failures.
- **FR-015**: The system MUST prevent a user from overwriting a ticket that was changed by someone
  else since they loaded it, and MUST tell them so.

### Key Entities

- **Ticket**: A support request. Has a unique identifier, title, description, status (Open, In
  Progress, Resolved, Closed), priority (Low, Medium, High, Critical), an optional assignee, created
  and last-updated timestamps, and zero or more comments.
- **Comment**: A note added to one ticket. Has text, author, and a creation timestamp. Belongs to
  exactly one ticket.
- **Assignee**: The person responsible for a ticket, identified by name. A ticket has at most one
  assignee at a time, and may have none.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A user can create a new ticket and see it in the list in under 1 minute.
- **SC-002**: A user can find a specific ticket by keyword or status filter in under 30 seconds in a
  list of 1,000 tickets.
- **SC-003**: The ticket list, ticket details and search results appear within 2 seconds for 95% of
  requests with up to 1,000 tickets.
- **SC-004**: 100% of invalid submissions (blank or over-length fields, unknown priority or status)
  are rejected without changing stored data.
- **SC-005**: In 100% of rejected submissions, the user sees a message that identifies the problem
  field and reason, with no raw technical error text.
- **SC-006**: 100% of tickets and comments created before a restart are still present afterwards.
- **SC-007**: 90% of first-time users complete "create a ticket" and "add a comment" without help.

## Assumptions

- Users are internal support staff (agents); customer self-service is out of scope for this feature.
- Sign-in, user accounts and role-based permissions are handled by a separate feature. For this
  feature every user is treated as a permitted support user, and the comment author and assignee are
  entered as plain names. This will be revisited when sign-in is specified, because the project
  constitution requires authentication on all endpoints other than login and health.
- Ticket statuses are the fixed set Open, In Progress, Resolved and Closed, and any status may move
  to any other. Status changes are included because filtering by status needs tickets to move
  between statuses.
- Priorities are the fixed set Low, Medium, High and Critical; the default priority on creation is
  Medium if the user does not choose one.
- Deleting tickets or comments, editing or deleting comments, file attachments, email notifications,
  SLAs, reporting and ticket history/audit trails are out of scope for this feature.
- Keyword search is a simple "contains" match on title and description; ranking and fuzzy matching
  are out of scope.
- Dates and times are shown in the user's local time zone and stored consistently across users.
- Ticket lists show 20 tickets per page by default.
- The system is used on a modern desktop web browser; dedicated mobile apps are out of scope.
