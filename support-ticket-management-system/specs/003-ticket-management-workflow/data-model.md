# Data Model: Ticket Management with Status Workflow

**Feature**: `003-ticket-management-workflow` | **Date**: 2026-11-22

## Entity: Ticket

| Field         | Type (portable SQL)         | Rules                                                                 |
|---------------|-----------------------------|-----------------------------------------------------------------------|
| `id`          | `BIGINT` identity, PK       | Unique, system-generated (FR-002)                                     |
| `title`       | `VARCHAR(150)` NOT NULL     | Not blank, max 150 (FR-021)                                           |
| `description` | `VARCHAR(5000)` NOT NULL    | Not blank, max 5,000 (FR-021)                                         |
| `status`      | `VARCHAR(20)` NOT NULL      | One of five values; `CHECK` constraint; always `OPEN` on create       |
| `priority`    | `VARCHAR(20)` NOT NULL      | `LOW`, `MEDIUM`, `HIGH`, `CRITICAL`; `CHECK` constraint; default `MEDIUM` |
| `assignee`    | `VARCHAR(100)` NULL         | Optional name; blank is stored as null; max 100                       |
| `created_at`  | `TIMESTAMP WITH TIME ZONE`  | Set once, stored in UTC                                               |
| `updated_at`  | `TIMESTAMP WITH TIME ZONE`  | Set on every accepted edit or status change (FR-006)                  |
| `version`     | `BIGINT` NOT NULL           | Optimistic-lock counter, incremented on every change (FR-023)         |

Relationships: one Ticket has zero or more Comments, ordered by `created_at` ascending.

Indexes:

- `idx_ticket_status_created (status, created_at DESC)`: status filter plus newest-first listing.
- `idx_ticket_created (created_at DESC)`: unfiltered newest-first listing.

Status constraint (portable SQL):

```sql
CONSTRAINT ck_ticket_status CHECK (status IN ('OPEN','IN_PROGRESS','RESOLVED','CLOSED','CANCELLED'))
```

## Entity: Comment

| Field        | Type                        | Rules                                                        |
|--------------|-----------------------------|--------------------------------------------------------------|
| `id`         | `BIGINT` identity, PK       |                                                              |
| `ticket_id`  | `BIGINT` NOT NULL, FK       | References `ticket(id)`; comments belong to exactly one ticket |
| `author`     | `VARCHAR(100)` NOT NULL     | Not blank, max 100 (plain name; no accounts yet)             |
| `text`       | `VARCHAR(2000)` NOT NULL    | Not blank, max 2,000 (FR-021)                                |
| `created_at` | `TIMESTAMP WITH TIME ZONE`  | Set once, stored in UTC                                      |

Index: `idx_comment_ticket_created (ticket_id, created_at)`.

Comments are never edited or deleted (out of scope) and are accepted on every ticket regardless of
status. Adding a comment does not change the ticket's `updated_at` or `version`.

## Value object: TicketStatus

Stored as `EnumType.STRING`.

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

Decision table for all 20 ordered pairs of different statuses (A = accepted, R = rejected):

| from \ to       | OPEN | IN_PROGRESS | RESOLVED | CLOSED | CANCELLED |
|-----------------|:----:|:-----------:|:--------:|:------:|:---------:|
| **OPEN**        |  n/a |      A      |    R     |   R    |     A     |
| **IN_PROGRESS** |  R   |     n/a     |    A     |   R    |     A     |
| **RESOLVED**    |  R   |      R      |   n/a    |   A    |     R     |
| **CLOSED**      |  R   |      R      |    R     |  n/a   |     R     |
| **CANCELLED**   |  R   |      R      |    R     |   R    |    n/a    |

`n/a` = same status: not a transition and not rejected (FR-018). Totals: 5 accepted, 15 rejected.

## Validation order

For the **status endpoint** (`PATCH /tickets/{id}/status`):

1. Status value is one of the five known values, else `400 VALIDATION_ERROR` (FR-022).
2. Ticket exists, else `404 TICKET_NOT_FOUND`.
3. Value equals the current status: nothing to do, return the ticket (FR-018).
4. Pair is in the table, else `409 INVALID_STATUS_TRANSITION` (FR-015, FR-017).
5. Apply with the conditional update; on 0 rows reload and repeat steps 3 and 4 (FR-019).

For the **update endpoint** (`PUT /tickets/{id}`):

1. Bean Validation on all fields, including `version` present and status/priority values known,
   else `400`.
2. Ticket exists, else `404`.
3. If `status` is present and differs from the current status, check the pair (step 4 above), else
   `409 INVALID_STATUS_TRANSITION`.
4. Save with `@Version`; a stale `version` gives `409 CONCURRENT_MODIFICATION` (FR-023).
5. Any rejection rolls back the whole request, so nothing changes (FR-016).

An accepted change sets the new values, `updated_at`, and increments `version` in the same
transaction (FR-006).

## Derived read models

- **TicketSummary** (list): `id`, `title`, `status`, `priority`, `assignee`, `createdAt`. Read via a
  projection; comments are never loaded for list requests.
- **TicketDetail**: all Ticket fields, `allowedNextStatuses[]` computed from the status table
  (empty for final statuses, FR-020), and `comments[]` oldest first.
- **TicketPage**: `content[]` of TicketSummary plus `page`, `size`, `totalElements`, `totalPages`.
