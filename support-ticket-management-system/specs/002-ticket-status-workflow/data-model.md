# Data Model: Ticket Status Workflow

**Feature**: `002-ticket-status-workflow` | **Date**: 2026-11-22

Entities and fields not changed by this feature are listed only where the status workflow depends on
them. Their full behavior is defined in `specs/001-support-ticket-management/spec.md`.

## Entity: Ticket

| Field         | Type (portable SQL)         | Rules                                                              |
|---------------|-----------------------------|--------------------------------------------------------------------|
| `id`          | `BIGINT` identity, PK       | Unique, system-generated (FR-002 of 001)                           |
| `title`       | `VARCHAR(150)` NOT NULL     | Not blank, max 150                                                 |
| `description` | `VARCHAR(5000)` NOT NULL    | Not blank, max 5,000                                               |
| `status`      | `VARCHAR(20)` NOT NULL      | One of the five values below; `CHECK` constraint; default `OPEN`   |
| `priority`    | `VARCHAR(20)` NOT NULL      | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`; default `MEDIUM`              |
| `assignee`    | `VARCHAR(100)` NULL         | Optional name                                                      |
| `created_at`  | `TIMESTAMP WITH TIME ZONE`  | Set once, stored in UTC                                            |
| `updated_at`  | `TIMESTAMP WITH TIME ZONE`  | Set on every accepted change (FR-007, FR-011)                      |
| `version`     | `BIGINT` NOT NULL           | Optimistic-lock counter; incremented by every update               |

Relationships: one Ticket has zero or more Comments (see below).

Indexes:

- `idx_ticket_status_created (status, created_at DESC)`: status filter plus newest-first listing.
- `idx_ticket_created (created_at DESC)`: unfiltered newest-first listing.

Status column constraint (portable SQL):

```sql
CONSTRAINT ck_ticket_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED','CANCELLED'))
```

Migration note: if the baseline feature (001) has not been implemented yet, the first Flyway
migration creates the table with all five values. If it was implemented first with four values, add
a migration that drops and recreates `ck_ticket_status` to include `CANCELLED`. Existing rows keep
their status.

## Entity: Comment (referenced, unchanged by this feature)

| Field        | Type                        | Rules                                   |
|--------------|-----------------------------|-----------------------------------------|
| `id`         | `BIGINT` identity, PK       |                                         |
| `ticket_id`  | `BIGINT` NOT NULL, FK       | References `ticket(id)`                 |
| `author`     | `VARCHAR(100)` NOT NULL     |                                         |
| `text`       | `VARCHAR(2000)` NOT NULL    | Not blank, max 2,000                    |
| `created_at` | `TIMESTAMP WITH TIME ZONE`  |                                         |

Index: `idx_comment_ticket_created (ticket_id, created_at)`.

Comments are not affected by status changes and remain allowed on every ticket (spec Assumptions).

## Value object: TicketStatus

Stored as `EnumType.STRING`. Final statuses have no allowed next statuses.

| Status        | Final | Allowed next statuses          |
|---------------|-------|--------------------------------|
| `OPEN`        | no    | `IN_PROGRESS`, `CANCELLED`     |
| `IN_PROGRESS` | no    | `RESOLVED`, `CANCELLED`        |
| `RESOLVED`    | no    | `CLOSED`                       |
| `CLOSED`      | yes   | none                           |
| `CANCELLED`   | yes   | none                           |

## State transitions

```text
OPEN ──► IN_PROGRESS ──► RESOLVED ──► CLOSED
 │            │
 └────────────┴──────► CANCELLED
```

Complete decision table for all 20 ordered pairs of different statuses (A = accepted, R = rejected):

| from \ to       | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|-----------------|:----:|:-----------:|:--------:|:------:|:---------:|
| **OPEN**        |  n/a |      A      |    R     |   R    |     A     |
| **IN_PROGRESS** |  R   |     n/a     |    A     |   R    |     A     |
| **RESOLVED**    |  R   |      R      |   n/a    |   A    |     R     |
| **CLOSED**      |  R   |      R      |    R     |  n/a   |     R     |
| **CANCELLED**   |  R   |      R      |    R     |   R    |    n/a    |

`n/a` = same status: not a transition; not rejected (FR-010). Totals: 5 accepted, 15 rejected.

Validation order when a status is supplied:

1. Value is one of the five known statuses, else `400 VALIDATION_ERROR` (FR-009).
2. Ticket exists, else `404 TICKET_NOT_FOUND`.
3. Value equals current status: no transition, nothing to check (FR-010).
4. Pair is in the table above, else `409 INVALID_STATUS_TRANSITION` (FR-006, FR-008).
5. Apply with the conditional update; on 0 rows re-run steps 3 and 4 against the reloaded ticket
   (FR-012).

An accepted transition sets `status`, `updated_at` and increments `version` in one statement
(FR-011). A rejected transition writes nothing (FR-007).

## Derived read model: TicketSummary (list)

`id`, `title`, `status`, `priority`, `assignee`, `createdAt`. Read through a projection so comments
are never loaded for list requests.

## Derived read model: TicketDetail

All Ticket fields, `comments[]` (oldest first), and `allowedNextStatuses[]` computed from the
status table (FR-013). Final statuses return an empty array.
