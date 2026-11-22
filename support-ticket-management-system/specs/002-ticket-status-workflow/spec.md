# Feature Specification: Ticket Status Workflow

**Feature Branch**: `002-ticket-status-workflow`

**Created**: 2026-11-22

**Status**: Superseded by [003-ticket-management-workflow](../003-ticket-management-workflow/spec.md), which merges this feature with 001.

**Input**: User description: "The following state machine must be enforced by the backend: OPEN → IN_PROGRESS → RESOLVED → CLOSED; OPEN → CANCELLED; IN_PROGRESS → CANCELLED. Invalid transitions must be rejected. Example: CLOSED → OPEN ❌, RESOLVED → OPEN ❌, CANCELLED → OPEN ❌"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Move a Ticket Forward Through Its Lifecycle (Priority: P1)

A support user progresses a ticket through its normal lifecycle: a new ticket is Open, work starts
and it becomes In Progress, the fix is delivered and it becomes Resolved, and once confirmed it
becomes Closed. Each step is allowed only from the status immediately before it.

**Why this priority**: The forward lifecycle is the normal path for every ticket. Without it,
tickets cannot reflect real progress.

**Independent Test**: Can be fully tested by creating a ticket and moving it Open → In Progress →
Resolved → Closed, confirming each step is accepted and the new status is shown.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, **When** the user changes its status to In Progress, **Then** the
   change is accepted and the ticket shows In Progress.
2. **Given** a ticket is In Progress, **When** the user changes its status to Resolved, **Then** the
   change is accepted and the ticket shows Resolved.
3. **Given** a ticket is Resolved, **When** the user changes its status to Closed, **Then** the
   change is accepted and the ticket shows Closed.
4. **Given** any accepted status change, **When** it is saved, **Then** the ticket's last-updated
   time is refreshed.

---

### User Story 2 - Reject Invalid Status Changes (Priority: P1)

The system refuses any status change that is not part of the allowed workflow, no matter which
client or screen requested it, and tells the user exactly why. A rejected change leaves the ticket
unchanged.

**Why this priority**: Enforcing the workflow is the purpose of this feature. If invalid changes
can slip through, ticket statuses cannot be trusted.

**Independent Test**: Can be fully tested by attempting every status change that is not in the
allowed list and confirming each one is rejected with a clear message and no change to the ticket.

**Acceptance Scenarios**:

1. **Given** a ticket is Closed, **When** a change to Open is requested, **Then** it is rejected.
2. **Given** a ticket is Resolved, **When** a change to Open is requested, **Then** it is rejected.
3. **Given** a ticket is Cancelled, **When** a change to Open is requested, **Then** it is rejected.
4. **Given** a ticket is Open, **When** a change straight to Resolved or Closed is requested (skipping
   a step), **Then** it is rejected.
5. **Given** a ticket is In Progress, **When** a change back to Open is requested, **Then** it is
   rejected.
6. **Given** a ticket is Resolved, **When** a change to Cancelled or In Progress is requested,
   **Then** it is rejected.
7. **Given** a ticket is Closed or Cancelled, **When** any change to a different status is
   requested, **Then** it is rejected because those statuses are final.
8. **Given** any rejected status change, **When** the response is shown, **Then** it states the
   current status, the requested status, and the statuses that are allowed next, and the ticket's
   status and last-updated time are unchanged.

---

### User Story 3 - Cancel a Ticket (Priority: P2)

A support user cancels a ticket that no longer needs work, such as a duplicate or a request
withdrawn by the customer. Cancellation is only possible while the ticket is Open or In Progress.

**Why this priority**: Cancellation covers tickets that should end without being resolved. It is
important, but the lifecycle works without it.

**Independent Test**: Can be fully tested by cancelling one Open ticket and one In Progress ticket
and confirming both become Cancelled, and that Resolved, Closed and already Cancelled tickets
cannot be cancelled.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, **When** the user cancels it, **Then** it becomes Cancelled.
2. **Given** a ticket is In Progress, **When** the user cancels it, **Then** it becomes Cancelled.
3. **Given** a ticket is Resolved, Closed or Cancelled, **When** the user tries to cancel it,
   **Then** the request is rejected with a clear message.
4. **Given** a ticket is Cancelled, **When** the user filters the list by status, **Then** it is
   found under the Cancelled filter and not under any other status.

---

### User Story 4 - Only Offer Valid Next Statuses in the UI (Priority: P3)

When a user looks at a ticket, the screen offers only the statuses the ticket may move to next, and
shows the system's explanation if a change is still rejected (for example because someone else
changed the ticket first).

**Why this priority**: This prevents mistakes and confusion, but the rules are already protected
without it. The system, not the screen, remains the authority.

**Independent Test**: Can be fully tested by opening tickets in each status and confirming the
available status choices match the allowed workflow, and that a final-status ticket offers none.

**Acceptance Scenarios**:

1. **Given** a ticket is Open, **When** the user views the status choices, **Then** only In Progress
   and Cancelled are offered.
2. **Given** a ticket is In Progress, **When** the user views the status choices, **Then** only
   Resolved and Cancelled are offered.
3. **Given** a ticket is Resolved, **When** the user views the status choices, **Then** only Closed
   is offered.
4. **Given** a ticket is Closed or Cancelled, **When** the user views the ticket, **Then** no status
   change is offered and the ticket is shown as final.
5. **Given** a change is rejected by the system, **When** the user sees the result, **Then** the
   message from the system is shown in plain language and the screen refreshes to the ticket's
   actual current status.

---

### Edge Cases

- Two users change the same ticket's status at the same time: only the first change is applied; the
  second is checked against the new status and is accepted or rejected accordingly, with a clear
  message.
- A status change is requested to the ticket's current status (for example Open to Open): it is not
  a transition and is treated as no status change; other edits in the same request are unaffected.
- A status value is not one of Open, In Progress, Resolved, Closed or Cancelled (for example a
  misspelling): it is rejected as an unknown status, listing the valid values, which is distinct
  from an invalid-transition message.
- A ticket is edited in other ways (title, description, priority, assignee) without a status change:
  the workflow rules do not apply to that edit.
- A new ticket is submitted with a status other than Open: the status is ignored or rejected, and
  the ticket is always created as Open.
- A status change is requested for a ticket that does not exist: the user sees a "ticket not found"
  message.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST support exactly five ticket statuses: Open, In Progress, Resolved,
  Closed and Cancelled.
- **FR-002**: Every new ticket MUST be created with the status Open, regardless of any status
  supplied by the user.
- **FR-003**: The system MUST allow only these status transitions, and no others:
  - Open → In Progress
  - In Progress → Resolved
  - Resolved → Closed
  - Open → Cancelled
  - In Progress → Cancelled
- **FR-004**: The system MUST treat Closed and Cancelled as final statuses; a ticket in a final
  status MUST NOT move to any other status.
- **FR-005**: The system MUST enforce FR-003 and FR-004 itself, so that no client, screen or
  direct request can bypass the rules.
- **FR-006**: The system MUST reject every transition not listed in FR-003, including at least:
  Closed → Open, Resolved → Open, Cancelled → Open, In Progress → Open, Open → Resolved,
  Open → Closed, In Progress → Closed, Resolved → In Progress and Resolved → Cancelled.
- **FR-007**: A rejected transition MUST leave the ticket completely unchanged, including its status
  and last-updated time.
- **FR-008**: A rejection MUST explain the problem using the ticket's current status, the requested
  status, and the list of statuses allowed next (or state that the status is final).
- **FR-009**: The system MUST distinguish an unknown status value (reported as invalid input with
  the list of valid values) from a known status that is not an allowed transition (reported as an
  invalid transition).
- **FR-010**: A request that keeps the ticket's current status MUST NOT be treated as a transition
  and MUST NOT be rejected for that reason.
- **FR-011**: An accepted transition MUST update the ticket's status and last-updated time, and the
  new status MUST be stored so it survives a system restart.
- **FR-012**: When two status changes for the same ticket arrive at the same time, the system MUST
  apply them one at a time, checking each against the ticket's status at that moment.
- **FR-013**: The system MUST make a ticket's allowed next statuses available so screens can offer
  only valid choices.
- **FR-014**: Users MUST be able to filter the ticket list by each of the five statuses, including
  Cancelled.
- **FR-015**: The user interface MUST offer only the allowed next statuses for a ticket, MUST offer
  none for a Closed or Cancelled ticket, and MUST show the system's rejection message in plain
  language when a change is refused.

### Key Entities

- **Ticket Status**: The stage a ticket is in. One of Open, In Progress, Resolved, Closed or
  Cancelled. Closed and Cancelled are final.
- **Status Transition Rule**: A permitted move from one status to another. The complete set of rules
  is defined in FR-003; any move not in the set is invalid.
- **Ticket**: The existing support request; this feature adds the Cancelled status and the rule set
  that governs how its status can change.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: 100% of the five allowed transitions are accepted when requested from the correct
  starting status.
- **SC-002**: 100% of all other status changes between different statuses (15 of the 20 possible
  ordered pairs) are rejected, with no change to the ticket.
- **SC-003**: 100% of rejection messages name the current status, the requested status, and the
  allowed next statuses (or say the status is final).
- **SC-004**: No ticket ever reaches a status from which a rejected transition has moved it, even
  when changes are submitted by something other than the normal screens.
- **SC-005**: Users see the result of a status change (accepted or rejected) within 2 seconds for
  95% of attempts.
- **SC-006**: Users never see an invalid status choice offered for a ticket in any status.
- **SC-007**: 90% of users change a ticket's status correctly on their first attempt without help.

## Assumptions

- This feature builds on the Support Ticket Management feature (`specs/001-support-ticket-management`).
  It **replaces** that feature's statuses and rules: that spec lists four statuses and allows any
  status to move to any other. This spec adds Cancelled and restricts transitions; the earlier spec
  (FR-006, Story 2 scenario 2, Assumptions, edge cases, filter wording) should be updated to match.
- Statuses are shown to users as Open, In Progress, Resolved, Closed and Cancelled; the underlying
  values are OPEN, IN_PROGRESS, RESOLVED, CLOSED and CANCELLED.
- Closed and Cancelled are final: tickets cannot be re-opened. Re-opening is out of scope; a new
  ticket should be raised instead.
- Only the status is constrained by this feature. Editing title, description, priority or assignee,
  and adding comments, work as defined in the earlier spec and are not blocked on final tickets;
  locking final tickets is out of scope.
- A cancellation reason, who changed the status, and status-change history or audit trail are out of
  scope.
- Any support user may perform any allowed transition; role-based restrictions on who can change
  status depend on the separate sign-in feature.
- Existing tickets with a status set from the earlier four-status model keep that status and follow
  the new rules from now on; no data migration beyond adding Cancelled is needed.
